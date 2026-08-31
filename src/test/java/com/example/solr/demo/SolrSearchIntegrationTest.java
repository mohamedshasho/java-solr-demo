package com.example.solr.demo;

import com.example.solr.demo.dto.SearchRequestDto;
import com.example.solr.demo.dto.SearchResponseDto;
import com.example.solr.demo.dto.SuggestResponseDto;
import com.example.solr.demo.repository.ProductRepository;
import com.example.solr.demo.service.DataSeederService;
import com.example.solr.demo.service.SolrSearchService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests verifying Solr integration, bilingual indexing,
 * and hard search test cases (Arabic normalization, stemming, fuzzy typos, autocomplete).
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SolrSearchIntegrationTest {

    @Autowired
    private SolrSearchService searchService;

    @Autowired
    private DataSeederService dataSeederService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeAll
    void setupCatalog() {
        // Ensure database and Solr are populated with products
        if (productRepository.count() < 100) {
            dataSeederService.seedProducts(500);
        }
    }

    @Test
    @DisplayName("Test 1: Basic Search across English titles")
    void testBasicEnglishSearch() {
        SearchRequestDto req = new SearchRequestDto("Samsung");
        SearchResponseDto res = searchService.search(req);

        assertNotNull(res);
        assertTrue(res.getTotalHits() > 0, "Should find Samsung products");
        assertFalse(res.getItems().isEmpty());
    }

    @Test
    @DisplayName("Test 2: Hard Test - Arabic Alef & Diacritic Normalization ('اجهزة' matches 'أجهزة')")
    void testArabicAlefNormalization() {
        // Searching without Hamza ('اجهزة') should match products indexed with Hamza ('أجهزة')
        SearchRequestDto req = new SearchRequestDto("اجهزة");
        SearchResponseDto res = searchService.search(req);

        assertNotNull(res);
        assertTrue(res.getTotalHits() > 0, "Solr ArabicNormalization should match 'اجهزة' to 'أجهزة'");
    }

    @Test
    @DisplayName("Test 3: Hard Test - Arabic Plural Stemming ('سماعات' matches 'سماعة')")
    void testArabicPluralStemming() {
        SearchRequestDto req = new SearchRequestDto("سماعات");
        SearchResponseDto res = searchService.search(req);

        assertNotNull(res);
        assertTrue(res.getTotalHits() > 0, "Solr ArabicStemmer should match plural 'سماعات'");
    }

    @Test
    @DisplayName("Test 4: Hard Test - English Porter Stemming ('wireless headphones')")
    void testEnglishStemming() {
        SearchRequestDto req = new SearchRequestDto("wireless headphones");
        SearchResponseDto res = searchService.search(req);

        assertNotNull(res);
        assertTrue(res.getTotalHits() > 0, "Solr PorterStemmer should match 'wireless headphones'");
    }

    @Test
    @DisplayName("Test 5: Hard Test - Typo Tolerance & Fuzzy Search ('samsng')")
    void testFuzzyTypoTolerance() {
        SearchRequestDto req = new SearchRequestDto("samsng");
        req.setFuzzy(true);
        SearchResponseDto res = searchService.search(req);

        assertNotNull(res);
        assertTrue(res.getTotalHits() > 0, "Fuzzy search should tolerate typo 'samsng' and find 'Samsung'");
    }

    @Test
    @DisplayName("Test 6: SKU Prefix Match ('ELEC-SAM')")
    void testSkuPrefixMatch() {
        SearchRequestDto req = new SearchRequestDto("ELEC-SAM");
        SearchResponseDto res = searchService.search(req);

        assertNotNull(res);
        assertTrue(res.getTotalHits() > 0, "Should match products with SKU prefix 'ELEC-SAM'");
    }

    @Test
    @DisplayName("Test 7: Autocomplete / Suggest Typeahead")
    void testAutocompleteSuggest() {
        SuggestResponseDto suggestRes = searchService.suggest("sam", 5);

        assertNotNull(suggestRes);
        assertNotNull(suggestRes.getSuggestions());
        assertFalse(suggestRes.getSuggestions().isEmpty(), "Should return autocomplete suggestions for prefix 'sam'");
    }

    @Test
    @DisplayName("Test 8: Operator AND vs OR in Multi-word Queries")
    void testOpAndVsOr() {
        // Multi-word query with op=AND
        SearchRequestDto reqAnd = new SearchRequestDto("Samsung Galaxy");
        reqAnd.setOp("AND");
        SearchResponseDto resAnd = searchService.search(reqAnd);

        assertNotNull(resAnd);
        assertTrue(resAnd.getTotalHits() > 0, "AND query should match documents containing both Samsung and Galaxy");

        // Multi-word query with op=OR
        SearchRequestDto reqOr = new SearchRequestDto("Samsung Apple");
        reqOr.setOp("OR");
        SearchResponseDto resOr = searchService.search(reqOr);

        assertNotNull(resOr);
        assertTrue(resOr.getTotalHits() >= resAnd.getTotalHits(), "OR query matching Samsung OR Apple should return broad hits");
    }

    @Test
    @DisplayName("Test 9: Real-time Solr Searcher Refresh")
    void testSolrIndexRefresh() {
        boolean refreshed = searchService.refreshIndex();
        assertTrue(refreshed, "Solr searcher refresh should succeed");
    }
}
