package com.example.solr.demo.controller;

import com.example.solr.demo.config.SolrConfig;
import com.example.solr.demo.dto.HardExampleDto;
import com.example.solr.demo.dto.StatsResponseDto;
import com.example.solr.demo.repository.ProductRepository;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller providing showcase metadata and live metrics for presentations.
 */
@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoShowcaseController {

    private final ProductRepository productRepository;
    private final SolrClient solrClient;
    private final SolrConfig solrConfig;

    public DemoShowcaseController(ProductRepository productRepository, SolrClient solrClient, SolrConfig solrConfig) {
        this.productRepository = productRepository;
        this.solrClient = solrClient;
        this.solrConfig = solrConfig;
    }

    /**
     * Returns curated "Hard Search Test Cases" explaining complex Solr features vs SQL limitations.
     */
    @GetMapping("/hard-examples")
    public ResponseEntity<List<HardExampleDto>> getHardExamples() {
        List<HardExampleDto> list = new ArrayList<>();

        list.add(new HardExampleDto(
                "ar_alef_norm",
                "Arabic Alef & Diacritic Normalization",
                "Arabic Morphology",
                "اجهزة ابل",
                "Arabic",
                "Solr 'arabicNormalization' filter folds (أ, إ, آ) into bare (ا). Searching 'اجهزة' matches 'أجهزة'.",
                "SQL LIKE '%اجهزة%' fails completely because SQL treats 'أ' and 'ا' as strictly distinct byte values.",
                "Apple iPhone 16 Pro Max, iPad, and Apple smart devices.",
                "emerald"
        ));

        list.add(new HardExampleDto(
                "ar_taa_marbuta",
                "Arabic Taa Marbuta / Haa Variation",
                "Arabic Morphology",
                "قهوه اسبريسو",
                "Arabic",
                "Solr normalizes 'ة' (Taa Marbuta) and 'ه' (Haa) interchangeably. Searching 'قهوه' finds 'قهوة'.",
                "SQL LIKE '%قهوه%' returns 0 results if the database stores 'قهوة'.",
                "Nespresso Vertuo Next Espresso Machine, DeLonghi espresso makers.",
                "emerald"
        ));

        list.add(new HardExampleDto(
                "ar_definite_article",
                "Arabic Definite Article (الـ) Stripping",
                "Arabic Stemming",
                "الاسبريسو",
                "Arabic",
                "Solr 'arabicStem' filter strips the leading 'الـ' prefix automatically during tokenization.",
                "SQL LIKE '%الاسبريسو%' misses records containing only 'اسبريسو'.",
                "Nespresso and Coffee machines with 'اسبريسو' or 'الاسبريسو'.",
                "blue"
        ));

        list.add(new HardExampleDto(
                "ar_plural_stemming",
                "Arabic Plural vs Singular Stemming",
                "Arabic Stemming",
                "سماعات",
                "Arabic",
                "Solr Arabic Stemmer maps plural forms ('سماعات') and singular forms ('سماعة') to the common root.",
                "SQL LIKE requires exact substring matches and misses irregular or singular forms.",
                "Sony WH-1000XM5, Apple AirPods Pro, Bose headphones.",
                "blue"
        ));

        list.add(new HardExampleDto(
                "en_stemming_plurals",
                "English Porter Stemming (Plurals & Tenses)",
                "English NLP",
                "wireless headphones",
                "English",
                "Solr 'porterStem' analyzer stems 'headphones' -> 'headphon' and 'wireless' -> 'wireless', matching both singular and plural forms.",
                "SQL LIKE '%wireless headphones%' misses singular entries like 'wireless headphone'.",
                "Sony WH-1000XM5, Apple AirPods, Bose Wireless Headphones.",
                "purple"
        ));

        list.add(new HardExampleDto(
                "en_running_verbs",
                "English Verb & Gerund Inflexions",
                "English NLP",
                "running shoes",
                "English",
                "Stemmer maps 'running', 'runner', and 'runs' to the base root 'run'.",
                "SQL LIKE fails when products are titled 'Road Run Shoe' or 'Runner Shoes'.",
                "Nike Air Zoom Pegasus 40, Adidas Running Shoes.",
                "purple"
        ));

        list.add(new HardExampleDto(
                "typo_fuzzy_brand",
                "Typo & Spelling Tolerance (Fuzzy Search)",
                "Typo Tolerance",
                "samsng galxy",
                "English / Typo",
                "Solr eDisMax / Levenshtein Damerau distance (~1 or ~2 edits) automatically finds 'Samsung Galaxy'.",
                "SQL LIKE '%samsng%' returns zero results with no suggestions.",
                "Samsung Galaxy S24 Ultra 5G, Galaxy Watches, Galaxy Buds.",
                "amber"
        ));

        list.add(new HardExampleDto(
                "typo_laptop_sound",
                "Common Typo Variation ('laptob')",
                "Typo Tolerance",
                "laptob gaming",
                "English / Typo",
                "Fuzzy matching tolerates 'b' instead of 'p' and matches 'laptop gaming'.",
                "SQL returns 0 matches.",
                "Dell XPS 15 Gaming Laptop, Asus ROG, Lenovo Legion.",
                "amber"
        ));

        list.add(new HardExampleDto(
                "sku_prefix_match",
                "SKU Code & Exact Prefix Search",
                "SKU Indexing",
                "ELEC-SAM",
                "Code / SKU",
                "Solr string field index allows fast sub-millisecond prefix B-tree lookups.",
                "SQL full table scan on 5,000+ unindexed strings is orders of magnitude slower.",
                "ELEC-SAM-S24U-001 (Samsung Galaxy S24 Ultra).",
                "indigo"
        ));

        list.add(new HardExampleDto(
                "cross_lingual_mixed",
                "Cross-Lingual Bilingual Mixed Queries",
                "Multi-Lingual",
                "Samsung جالكسي",
                "Mixed (EN + AR)",
                "eDisMax evaluates English and Arabic fields simultaneously with weighted boosts across both languages.",
                "SQL requires complex multi-table OR joins with full table scans.",
                "Samsung Galaxy S24 Ultra and Samsung smartphone accessories.",
                "rose"
        ));

        return ResponseEntity.ok(list);
    }

    /**
     * Live metrics endpoint for presentation dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<StatsResponseDto> getStats() {
        StatsResponseDto stats = new StatsResponseDto();
        stats.setDatabaseProductCount(productRepository.count());
        stats.setSolrBaseUrl(solrConfig.getSolrBaseUrl());
        stats.setSolrCoreName(solrConfig.getSolrCoreName());
        stats.setCategories(productRepository.findDistinctCategoriesEn());
        stats.setBrands(productRepository.findDistinctBrands());

        try {
            SolrQuery q = new SolrQuery("*:*");
            q.setRows(0);
            QueryResponse resp = solrClient.query(solrConfig.getSolrCoreName(), q);
            stats.setSolrIndexedDocCount(resp.getResults().getNumFound());
            stats.setSolrConnected(true);
        } catch (Exception e) {
            stats.setSolrConnected(false);
            stats.setSolrIndexedDocCount(-1);
        }

        return ResponseEntity.ok(stats);
    }
}
