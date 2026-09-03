package com.example.solr.demo.service;

import com.example.solr.demo.config.SolrConfig;
import com.example.solr.demo.document.ProductSolrDoc;
import com.example.solr.demo.dto.*;
import com.example.solr.demo.entity.Product;
import com.example.solr.demo.repository.ProductRepository;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-performance search service leveraging Apache Solr 10's eDisMax Query Parser.
 * <p>
 * Presentation Concepts Illustrated:
 * -----------------------------------
 * 1. eDisMax (Extended DisMax) Parser:
 *    - Tolerates punctuation and syntax errors in user queries without throwing parsing exceptions.
 *    - Applies field boosting (e.g., matching a product title is 4x more relevant than matching description).
 * 2. Multi-Lingual Analyzers:
 *    - 'text_en' Porter Stemmer normalizes English words (e.g., "running", "runs" -> "run").
 *    - 'text_ar' Arabic Stemmer & Normalizer harmonizes variations of Alef (أ, إ, آ -> ا),
 *      Taa-Marbuta (ة -> ه), and handles Arabic prefix removal (الـ).
 * 3. Highlighting:
 *    - Solr extracts the exact snippet where the match occurred and wraps it in &lt;mark&gt; HTML tags.
 * 4. Hybrid Search Architecture:
 *    - Solr returns matching IDs + scores in &lt; 5ms.
 *    - Batch-fetches full relational data from DB by IDs for complete product presentation cards.
 */
@Service
public class SolrSearchService {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchService.class);

    private final SolrClient solrClient;
    private final SolrConfig solrConfig;
    private final ProductRepository productRepository;

    public SolrSearchService(SolrClient solrClient, SolrConfig solrConfig, ProductRepository productRepository) {
        this.solrClient = solrClient;
        this.solrConfig = solrConfig;
        this.productRepository = productRepository;
    }

    /**
     * Executes multi-field weighted search using Solr eDisMax parser.
     *
     * @param request Search request parameters (q, page, size, fuzzy, highlight, lang)
     * @return Full SearchResponseDto with Solr docs, highlight snippets, execution time, and DB data
     */
    public SearchResponseDto search(SearchRequestDto request) {
        SearchResponseDto response = new SearchResponseDto();
        String rawQuery = (request.getQ() != null && !request.getQ().trim().isEmpty()) ? request.getQ().trim() : "*:*";
        response.setQuery(rawQuery);
        response.setPage(request.getPage());
        response.setSize(request.getSize());

        String coreName = solrConfig.getSolrCoreName();
        SolrQuery solrQuery = new SolrQuery();

        // 1. Configure Query & Pagination
        solrQuery.setStart(request.getPage() * request.getSize());
        solrQuery.setRows(request.getSize());

        if ("*:*".equals(rawQuery)) {
            solrQuery.setQuery("*:*");
        } else {
            // Apply eDisMax (Extended Disjunction Max) Query Parser
            solrQuery.set("defType", "edismax");

            String queryText = rawQuery;
            if (request.isFuzzy() && !rawQuery.contains("~") && !rawQuery.contains("*")) {
                // Apply Levenshtein fuzzy distance matching (~1 edit distance)
                String[] words = rawQuery.split("\\s+");
                queryText = Arrays.stream(words)
                        .filter(w -> !w.isEmpty())
                        .map(w -> w.length() > 3 ? w + "~1" : w)
                        .collect(Collectors.joining(" "));
            }
            solrQuery.setQuery(queryText);

            // 2. Configure Field Weights (Query Fields with Boosts)
            // Title & SKU matches are given substantially higher priority than descriptions
            String lang = request.getLang();
            if ("en".equalsIgnoreCase(lang)) {
                solrQuery.set("qf", "sku^6.0 title_en^4.0 short_description_en^1.5 suggest_text^3.0");
            } else if ("ar".equalsIgnoreCase(lang)) {
                solrQuery.set("qf", "sku^6.0 title_ar^4.0 short_description_ar^1.5 suggest_text^3.0");
            } else {
                // Multi-lingual search across both English and Arabic
                solrQuery.set("qf", "sku^6.0 title_en^4.0 title_ar^4.0 short_description_en^1.5 short_description_ar^1.5 suggest_text^3.0");
            }

            // 3. Configure Default Query Operator (q.op=AND or q.op=OR)
            // When op=AND, mm=100% requiring all search tokens to match across fields
            String op = request.getOp();
            solrQuery.set("q.op", op);
            if ("AND".equalsIgnoreCase(op)) {
                solrQuery.set("mm", "100%");
            } else {
                solrQuery.set("mm", "1");
            }

            // Phrase Boost (exact phrase matching gets additional boost)
            solrQuery.set("pf", "title_en^8.0 title_ar^8.0 short_description_en^3.0 short_description_ar^3.0");
            solrQuery.set("ps", "2"); // phrase slop of 2 words
        }

        // 3. Configure Highlighting Black
        if (request.isHighlight() && !"*:*".equals(rawQuery)) {
            solrQuery.setHighlight(true);
            solrQuery.addHighlightField("title_en");
            solrQuery.addHighlightField("title_ar");
            solrQuery.addHighlightField("short_description_en");
            solrQuery.addHighlightField("short_description_ar");
            solrQuery.setHighlightSimplePre("<mark class=\"highlight-term\">");
            solrQuery.setHighlightSimplePost("</mark>");
            solrQuery.setHighlightSnippets(1);
            solrQuery.setHighlightFragsize(120);
        }

        // Include score in returned fields
        solrQuery.setFields("id", "sku", "title_en", "title_ar", "short_description_en", "short_description_ar", "score");

        try {
            long startTime = System.currentTimeMillis();
            QueryResponse solrResponse = solrClient.query(coreName, solrQuery);
            long elapsed = System.currentTimeMillis() - startTime;

            SolrDocumentList docs = solrResponse.getResults();
            long totalHits = docs.getNumFound();

            response.setTotalHits(totalHits);
            response.setQTimeMs(solrResponse.getQTime() >= 0 ? solrResponse.getQTime() : elapsed);
            response.setParsedSolrQuery(solrQuery.toString());
            response.setTotalPages((int) Math.ceil((double) totalHits / request.getSize()));

            // Extract Highlighting Map
            Map<String, Map<String, List<String>>> highlighting = solrResponse.getHighlighting();

            // 4. Batch-load DB entities for enriched presentation cards
            List<Long> docIds = new ArrayList<>();
            for (SolrDocument doc : docs) {
                String idStr = (String) doc.getFieldValue("id");
                if (idStr != null) {
                    try {
                        docIds.add(Long.parseLong(idStr));
                    } catch (NumberFormatException ignored) {}
                }
            }

            Map<Long, Product> productDbMap = Collections.emptyMap();
            if (!docIds.isEmpty()) {
                List<Product> dbProducts = productRepository.findAllById(docIds);
                productDbMap = dbProducts.stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
            }

            // 5. Construct SearchResultItemDto list
            List<SearchResultItemDto> items = new ArrayList<>();
            for (SolrDocument doc : docs) {
                SearchResultItemDto item = new SearchResultItemDto();
                String idStr = (String) doc.getFieldValue("id");
                item.setId(idStr);
                item.setSku((String) doc.getFieldValue("sku"));
                item.setTitleEn((String) doc.getFieldValue("title_en"));
                item.setTitleAr((String) doc.getFieldValue("title_ar"));
                item.setShortDescriptionEn((String) doc.getFieldValue("short_description_en"));
                item.setShortDescriptionAr((String) doc.getFieldValue("short_description_ar"));

                Object scoreObj = doc.getFieldValue("score");
                if (scoreObj instanceof Float) {
                    item.setScore((Float) scoreObj);
                } else if (scoreObj instanceof Number) {
                    item.setScore(((Number) scoreObj).floatValue());
                }

                // Attach Solr highlight snippets
                if (highlighting != null && idStr != null && highlighting.containsKey(idStr)) {
                    Map<String, List<String>> docHighlights = highlighting.get(idStr);
                    item.setHighlights(docHighlights);

                    // Determine best highlighted title
                    if (docHighlights.containsKey("title_en") && !docHighlights.get("title_en").isEmpty()) {
                        item.setHighlightedTitle(docHighlights.get("title_en").get(0));
                    } else if (docHighlights.containsKey("title_ar") && !docHighlights.get("title_ar").isEmpty()) {
                        item.setHighlightedTitle(docHighlights.get("title_ar").get(0));
                    }

                    // Determine best highlighted description
                    if (docHighlights.containsKey("short_description_en") && !docHighlights.get("short_description_en").isEmpty()) {
                        item.setHighlightedDescription(docHighlights.get("short_description_en").get(0));
                    } else if (docHighlights.containsKey("short_description_ar") && !docHighlights.get("short_description_ar").isEmpty()) {
                        item.setHighlightedDescription(docHighlights.get("short_description_ar").get(0));
                    }
                }

                // Enrich with Database attributes
                if (idStr != null) {
                    try {
                        Long pId = Long.parseLong(idStr);
                        Product dbProduct = productDbMap.get(pId);
                        if (dbProduct != null) {
                            item.setPrice(dbProduct.getPrice());
                            item.setCurrency(dbProduct.getCurrency());
                            item.setBrand(dbProduct.getBrand());
                            item.setCategoryEn(dbProduct.getCategoryEn());
                            item.setCategoryAr(dbProduct.getCategoryAr());
                            item.setInStock(dbProduct.getInStock());
                            item.setStockQuantity(dbProduct.getStockQuantity());
                            item.setRating(dbProduct.getRating());
                            item.setReviewsCount(dbProduct.getReviewsCount());
                            item.setImageUrl(dbProduct.getImageUrl());
                        }
                    } catch (NumberFormatException ignored) {}
                }

                items.add(item);
            }

            response.setItems(items);

            // 6. If 0 hits and not already fuzzy, check spellcheck / suggestions
            if (totalHits == 0 && !"*:*".equals(rawQuery)) {
                SpellcheckResponseDto spell = spellcheck(rawQuery);
                if (spell.getDidYouMean() != null) {
                    response.setDidYouMean(spell.getDidYouMean());
                }
                response.setSpellcheckSuggestions(spell.getSuggestions());
            }

        } catch (Exception e) {
            log.error("Solr search query failed: {}", e.getMessage(), e);
            response.setParsedSolrQuery("Error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Autocomplete / Typeahead suggestion endpoint.
     * Fast sub-millisecond lookup against edge-ngram and prefix fields.
     */
    public SuggestResponseDto suggest(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return new SuggestResponseDto(prefix, 0, Collections.emptyList());
        }

        String term = prefix.trim();
        String coreName = solrConfig.getSolrCoreName();
        SolrQuery solrQuery = new SolrQuery();

        // Query against suggest_text, title_en, title_ar and sku
        String cleanTerm = term.replaceAll("[+\\-&|!(){}\\[\\]^\"~*?:\\\\/]", "");
        solrQuery.setQuery(String.format("suggest_text:%s* OR title_en:%s* OR title_ar:%s* OR sku:%s*",
                cleanTerm, cleanTerm, cleanTerm, cleanTerm));
        solrQuery.setRows(Math.min(limit > 0 ? limit : 10, 20));
        solrQuery.setFields("id", "sku", "title_en", "title_ar");

        try {
            long start = System.currentTimeMillis();
            QueryResponse resp = solrClient.query(coreName, solrQuery);
            long elapsed = System.currentTimeMillis() - start;

            List<SuggestResponseDto.SuggestionItem> suggestions = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (SolrDocument doc : resp.getResults()) {
                String id = (String) doc.getFieldValue("id");
                String titleEn = (String) doc.getFieldValue("title_en");
                String titleAr = (String) doc.getFieldValue("title_ar");
                String sku = (String) doc.getFieldValue("sku");

                // Check English title match
                if (titleEn != null && titleEn.toLowerCase().contains(term.toLowerCase()) && seen.add(titleEn)) {
                    suggestions.add(new SuggestResponseDto.SuggestionItem(titleEn, "title_en", "English Title", id));
                }
                // Check Arabic title match
                if (titleAr != null && titleAr.contains(term) && seen.add(titleAr)) {
                    suggestions.add(new SuggestResponseDto.SuggestionItem(titleAr, "title_ar", "Arabic Title", id));
                }
                // Check SKU match
                if (sku != null && sku.toUpperCase().contains(term.toUpperCase()) && seen.add(sku)) {
                    suggestions.add(new SuggestResponseDto.SuggestionItem(sku, "sku", "Product SKU", id));
                }
            }

            return new SuggestResponseDto(term, resp.getQTime() >= 0 ? resp.getQTime() : elapsed, suggestions);
        } catch (Exception e) {
            log.error("Autocomplete suggest failed: {}", e.getMessage());
            return new SuggestResponseDto(term, 0, Collections.emptyList());
        }
    }

    /**
     * Spellcheck and "Did you mean?" suggestions.
     * Uses fuzzy search on title terms to discover corrections.
     */
    public SpellcheckResponseDto spellcheck(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new SpellcheckResponseDto(query, true, null, Collections.emptyList(), 0);
        }

        String raw = query.trim();
        String coreName = solrConfig.getSolrCoreName();
        SolrQuery solrQuery = new SolrQuery();

        // Search with fuzzy edit distance ~1 or ~2
        String[] tokens = raw.split("\\s+");
        String fuzzyQuery = Arrays.stream(tokens)
                .map(t -> t + "~2")
                .collect(Collectors.joining(" "));

        solrQuery.setQuery(fuzzyQuery);
        solrQuery.set("defType", "edismax");
        solrQuery.set("qf", "title_en^3.0 title_ar^3.0 sku^5.0");
        solrQuery.setRows(5);
        solrQuery.setFields("id", "title_en", "title_ar", "sku");

        try {
            long start = System.currentTimeMillis();
            QueryResponse resp = solrClient.query(coreName, solrQuery);
            long elapsed = System.currentTimeMillis() - start;

            List<String> suggestions = new ArrayList<>();
            for (SolrDocument doc : resp.getResults()) {
                String titleEn = (String) doc.getFieldValue("title_en");
                String titleAr = (String) doc.getFieldValue("title_ar");
                if (titleEn != null && !suggestions.contains(titleEn)) {
                    suggestions.add(titleEn);
                }
                if (titleAr != null && !suggestions.contains(titleAr)) {
                    suggestions.add(titleAr);
                }
            }

            String didYouMean = suggestions.isEmpty() ? null : suggestions.get(0);
            boolean correctlySpelled = suggestions.isEmpty();

            return new SpellcheckResponseDto(raw, correctlySpelled, didYouMean, suggestions, resp.getQTime() >= 0 ? resp.getQTime() : elapsed);
        } catch (Exception e) {
            log.error("Spellcheck query failed: {}", e.getMessage());
            return new SpellcheckResponseDto(raw, true, null, Collections.emptyList(), 0);
        }
    }

    /**
     * Explicitly refreshes the Apache Solr core index and opens a new searcher,
     * ensuring immediate real-time visibility for all newly added, updated, or deleted documents.
     *
     * @return true if refresh and commit succeeded
     */
    public boolean refreshIndex() {
        try {
            String coreName = solrConfig.getSolrCoreName();
            solrClient.commit(coreName, true, true);
            log.info("Successfully refreshed Apache Solr core '{}' and opened new searcher.", coreName);
            return true;
        } catch (Exception e) {
            log.error("Failed to refresh Solr index: {}", e.getMessage(), e);
            return false;
        }
    }
}
