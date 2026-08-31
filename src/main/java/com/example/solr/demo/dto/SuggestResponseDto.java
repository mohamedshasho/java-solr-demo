package com.example.solr.demo.dto;

import java.util.List;

/**
 * Autocomplete / Typeahead suggestion response.
 */
public class SuggestResponseDto {

    private String query;
    private long qTimeMs;
    private List<SuggestionItem> suggestions;

    public SuggestResponseDto() {
    }

    public SuggestResponseDto(String query, long qTimeMs, List<SuggestionItem> suggestions) {
        this.query = query;
        this.qTimeMs = qTimeMs;
        this.suggestions = suggestions;
    }

    public static class SuggestionItem {
        private String term;
        private String sourceField; // "title_en", "title_ar", "sku"
        private String type;        // "title", "sku", "category"
        private String productId;

        public SuggestionItem() {
        }

        public SuggestionItem(String term, String sourceField, String type, String productId) {
            this.term = term;
            this.sourceField = sourceField;
            this.type = type;
            this.productId = productId;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String getSourceField() {
            return sourceField;
        }

        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getQTimeMs() {
        return qTimeMs;
    }

    public void setQTimeMs(long qTimeMs) {
        this.qTimeMs = qTimeMs;
    }

    public List<SuggestionItem> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<SuggestionItem> suggestions) {
        this.suggestions = suggestions;
    }
}
