package com.example.solr.demo.controller;

import com.example.solr.demo.dto.SearchRequestDto;
import com.example.solr.demo.dto.SearchResponseDto;
import com.example.solr.demo.dto.SpellcheckResponseDto;
import com.example.solr.demo.dto.SuggestResponseDto;
import com.example.solr.demo.service.SolrSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing Apache Solr Search capabilities.
 * <p>
 * Endpoints:
 * - GET /api/products/search      : Multi-lingual eDisMax search with field boosting, highlighting &amp; fuzzy support
 * - GET /api/products/suggest     : Instant typeahead autocomplete powered by Solr edge-ngram/prefix indexes
 * - GET /api/products/autocomplete: Alias for autocomplete
 * - GET /api/products/spellcheck  : Solr spellchecking and 'Did you mean?' query corrections
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductSearchController {

    private final SolrSearchService searchService;

    public ProductSearchController(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Executes enterprise full-text search across localized fields in Apache Solr.
     *
     * @param q         Search keywords (English, Arabic, SKU, or mixed)
     * @param page      Page index (0-based)
     * @param size      Items per page
     * @param fuzzy     Enable Levenshtein typo tolerance (~1 edit distance)
     * @param highlight Enable keyword match snippet highlighting (&lt;mark&gt;)
     * @param lang      Filter by language: "all", "en", "ar"
     * @param sortBy    Sort criterion: "score", "price_asc", "price_desc"
     * @return SearchResponseDto with matched documents, score, highlights, and enriched DB attributes
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponseDto> search(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(name = "fuzzy", required = false, defaultValue = "false") boolean fuzzy,
            @RequestParam(name = "highlight", required = false, defaultValue = "true") boolean highlight,
            @RequestParam(name = "lang", required = false, defaultValue = "all") String lang,
            @RequestParam(name = "sortBy", required = false, defaultValue = "score") String sortBy,
            @RequestParam(name = "op", required = false, defaultValue = "AND") String op,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) String brand) {

        SearchRequestDto request = new SearchRequestDto();
        request.setQ(q);
        request.setPage(page);
        request.setSize(size);
        request.setFuzzy(fuzzy);
        request.setHighlight(highlight);
        request.setLang(lang);
        request.setSortBy(sortBy);
        request.setOp(op);
        request.setCategory(category);
        request.setBrand(brand);

        SearchResponseDto result = searchService.search(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Fast typeahead autocomplete endpoint for search input dropdowns.
     * Returns matching prefixes across English titles, Arabic titles, and SKUs.
     *
     * @param q     Prefix string typed by user
     * @param limit Max number of suggestions (default 10)
     */
    @GetMapping({"/suggest", "/autocomplete"})
    public ResponseEntity<SuggestResponseDto> suggest(
            @RequestParam("q") String q,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        SuggestResponseDto suggestions = searchService.suggest(q, limit);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Spellcheck endpoint returning correction suggestions ("Did you mean?").
     *
     * @param q Query with potential misspellings or typos
     */
    @GetMapping("/spellcheck")
    public ResponseEntity<SpellcheckResponseDto> spellcheck(@RequestParam("q") String q) {
        SpellcheckResponseDto spellcheckResult = searchService.spellcheck(q);
        return ResponseEntity.ok(spellcheckResult);
    }
}
