package com.example.solr.demo.service;

import com.example.solr.demo.config.SolrConfig;
import com.example.solr.demo.document.ProductSolrDoc;
import com.example.solr.demo.dto.ProductDetailDto;
import com.example.solr.demo.entity.Product;
import com.example.solr.demo.repository.ProductRepository;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing Product Database CRUD lifecycle and Dual-Write Synchronization with Apache Solr.
 * <p>
 * Architectural Note for Presentation:
 * ------------------------------------
 * Dual-write pattern: Whenever products are created, updated, or removed in the relational database,
 * an index update is dispatched to Apache Solr so search results remain synchronized in real time.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final SolrClient solrClient;
    private final SolrConfig solrConfig;

    public ProductService(ProductRepository productRepository, SolrClient solrClient, SolrConfig solrConfig) {
        this.productRepository = productRepository;
        this.solrClient = solrClient;
        this.solrConfig = solrConfig;
    }

    /**
     * Retrieves paginated products from the relational database.
     */
    public Page<ProductDetailDto> findAll(int page, int size) {
        Page<Product> productPage = productRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return productPage.map(ProductDetailDto::fromEntity);
    }

    /**
     * Retrieves a single product by its database primary key.
     */
    public Optional<ProductDetailDto> findById(Long id) {
        return productRepository.findById(id).map(ProductDetailDto::fromEntity);
    }

    /**
     * Creates a new product in the relational database and immediately indexes it into Solr.
     */
    @Transactional
    public ProductDetailDto createProduct(ProductDetailDto dto) {
        Product entity = dto.toEntity();
        Product saved = productRepository.save(entity);

        // Synchronize with Solr
        indexProductInSolr(saved);

        log.info("Created and indexed product ID={}, SKU={}", saved.getId(), saved.getSku());
        return ProductDetailDto.fromEntity(saved);
    }

    /**
     * Updates an existing product in the relational database and refreshes its Solr index.
     */
    @Transactional
    public Optional<ProductDetailDto> updateProduct(Long id, ProductDetailDto dto) {
        return productRepository.findById(id).map(existing -> {
            existing.setTitleEn(dto.getTitleEn());
            existing.setTitleAr(dto.getTitleAr());
            existing.setShortDescriptionEn(dto.getShortDescriptionEn());
            existing.setShortDescriptionAr(dto.getShortDescriptionAr());
            existing.setFullDescriptionEn(dto.getFullDescriptionEn());
            existing.setFullDescriptionAr(dto.getFullDescriptionAr());
            existing.setCategoryEn(dto.getCategoryEn());
            existing.setCategoryAr(dto.getCategoryAr());
            existing.setBrand(dto.getBrand());
            existing.setPrice(dto.getPrice());
            existing.setStockQuantity(dto.getStockQuantity());
            existing.setInStock(dto.getStockQuantity() != null && dto.getStockQuantity() > 0);
            existing.setRating(dto.getRating());
            existing.setReviewsCount(dto.getReviewsCount());
            existing.setImageUrl(dto.getImageUrl());
            existing.setTags(dto.getTags());

            Product updated = productRepository.save(existing);
            indexProductInSolr(updated);

            log.info("Updated and re-indexed product ID={}", updated.getId());
            return ProductDetailDto.fromEntity(updated);
        });
    }

    /**
     * Deletes a product from the relational database and purges it from the Solr index.
     */
    @Transactional
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            deleteProductFromSolr(id);
            log.info("Deleted product ID={} from DB and Solr", id);
            return true;
        }
        return false;
    }

    /**
     * Re-indexes all products from the database into Apache Solr in high-throughput batches.
     */
    public long reindexAll() {
        String coreName = solrConfig.getSolrCoreName();
        long count = productRepository.count();
        log.info("Starting complete re-indexing of {} products into Solr core '{}'...", count, coreName);

        try {
            // Clear existing Solr index
            solrClient.deleteByQuery(coreName, "*:*");

            int pageSize = 500;
            int pageNumber = 0;
            Page<Product> page;
            long totalIndexed = 0;

            do {
                page = productRepository.findAll(PageRequest.of(pageNumber, pageSize));
                List<ProductSolrDoc> docs = new ArrayList<>();
                for (Product p : page.getContent()) {
                    docs.add(toSolrDoc(p));
                }

                if (!docs.isEmpty()) {
                    solrClient.addBeans(coreName, docs);
                    totalIndexed += docs.size();
                }
                pageNumber++;
            } while (page.hasNext());

            solrClient.commit(coreName);
            log.info("Successfully re-indexed {} products into Solr core '{}'.", totalIndexed, coreName);
            return totalIndexed;
        } catch (Exception e) {
            log.error("Failed to reindex products into Solr: {}", e.getMessage(), e);
            throw new RuntimeException("Re-indexing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Indexes a single product into Apache Solr.
     */
    public void indexProductInSolr(Product product) {
        try {
            String coreName = solrConfig.getSolrCoreName();
            ProductSolrDoc doc = toSolrDoc(product);
            solrClient.addBean(coreName, doc);
            solrClient.commit(coreName);
        } catch (Exception e) {
            log.error("Failed to index product ID={} in Solr: {}", product.getId(), e.getMessage());
        }
    }

    /**
     * Deletes a product document from Solr by ID.
     */
    public void deleteProductFromSolr(Long id) {
        try {
            String coreName = solrConfig.getSolrCoreName();
            solrClient.deleteById(coreName, String.valueOf(id));
            solrClient.commit(coreName);
        } catch (Exception e) {
            log.error("Failed to delete product ID={} from Solr: {}", id, e.getMessage());
        }
    }

    /**
     * Maps database {@link Product} entity to lean {@link ProductSolrDoc}.
     */
    public ProductSolrDoc toSolrDoc(Product p) {
        return new ProductSolrDoc(
                String.valueOf(p.getId()),
                p.getSku(),
                p.getTitleEn(),
                p.getTitleAr(),
                p.getShortDescriptionEn(),
                p.getShortDescriptionAr()
        );
    }

    /**
     * Explicitly commits and opens a new searcher on the Solr core, refreshing search index visibility.
     */
    public boolean refreshSolrCore() {
        try {
            String coreName = solrConfig.getSolrCoreName();
            solrClient.commit(coreName, true, true);
            log.info("Solr core '{}' searcher refreshed successfully.", coreName);
            return true;
        } catch (Exception e) {
            log.error("Failed to refresh Solr core: {}", e.getMessage(), e);
            return false;
        }
    }
}
