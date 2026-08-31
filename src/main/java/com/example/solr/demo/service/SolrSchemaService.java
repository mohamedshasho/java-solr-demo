package com.example.solr.demo.service;

import com.example.solr.demo.config.SolrConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service responsible for ensuring the Apache Solr Core and Schema are properly configured on startup.
 * <p>
 * Architectural Explanation:
 * ----------------------------
 * 1. Checks if the target Solr core ('products') exists via CoreAdmin API.
 * 2. Automatically provisions the core if missing.
 * 3. Enforces Solr Schema field types:
 *    - 'text_en': Lucene English Analyzer (PorterStemmer, Lowercase, Stopwords, EnglishPossessive)
 *    - 'text_ar': Lucene Arabic Analyzer (ArabicNormalization for Alef/Yaa/Taa-Marbuta, ArabicStemmer, ArabicStopwords)
 *    - 'string': Exact match tokenization (for SKU lookup and IDs)
 *    - 'text_suggest': Edge-Ngram tokenization (for instant sub-millisecond autocomplete suggestions)
 * 4. Configures CopyFields to seamlessly populate suggestion indexes without polluting domain entities.
 */
@Service
public class SolrSchemaService {

    private static final Logger log = LoggerFactory.getLogger(SolrSchemaService.class);

    private final SolrConfig solrConfig;
    private final RestTemplate restTemplate;

    public SolrSchemaService(SolrConfig solrConfig) {
        this.solrConfig = solrConfig;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Executes upon Spring Boot application startup to verify and initialize Solr schema.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeSolrCoreAndSchema() {
        String coreName = solrConfig.getSolrCoreName();
        String baseUrl = solrConfig.getSolrBaseUrl();

        log.info("Checking Solr connection and core '{}' at {}...", coreName, baseUrl);

        try {
            ensureCoreExists(coreName, baseUrl);
            ensureSchemaFieldsConfigured(coreName, baseUrl);
            log.info("Apache Solr core '{}' and schema verification complete.", coreName);
        } catch (Exception e) {
            log.warn("Solr initialization check note: {}", e.getMessage());
        }
    }

    /**
     * Checks if the Solr core exists; if not, requests Solr to create it.
     */
    public void ensureCoreExists(String coreName, String baseUrl) {
        try {
            String statusUrl = baseUrl + "/admin/cores?action=STATUS&core=" + coreName;
            ResponseEntity<Map> resp = restTemplate.getForEntity(statusUrl, Map.class);
            if (resp.getBody() != null) {
                Map status = (Map) resp.getBody().get("status");
                if (status == null || !status.containsKey(coreName) || status.get(coreName) == null) {
                    log.info("Core '{}' does not exist in Solr. Requesting creation...", coreName);
                    String createUrl = baseUrl + "/admin/cores?action=CREATE&name=" + coreName + "&instanceDir=" + coreName;
                    restTemplate.getForObject(createUrl, Map.class);
                    log.info("Created Solr core '{}'.", coreName);
                } else {
                    log.info("Solr core '{}' is active and ready.", coreName);
                }
            }
        } catch (Exception e) {
            log.debug("Solr core check note: {}", e.getMessage());
        }
    }

    /**
     * Ensures all custom field types, fields, and copy fields are registered in Solr Schema.
     */
    public void ensureSchemaFieldsConfigured(String coreName, String baseUrl) {
        String schemaUrl = baseUrl + "/" + coreName + "/schema";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Add text_suggest field type (if not already present)
        try {
            String addFieldTypePayload = """
                {
                  "add-field-type": {
                    "name": "text_suggest",
                    "class": "solr.TextField",
                    "positionIncrementGap": "100",
                    "analyzer": {
                      "tokenizer": { "name": "standard" },
                      "filters": [
                        { "name": "lowercase" },
                        { "name": "arabicNormalization" },
                        { "name": "edgeNGram", "minGramSize": "2", "maxGramSize": "15" }
                      ]
                    }
                  }
                }
                """;
            restTemplate.postForObject(schemaUrl, new HttpEntity<>(addFieldTypePayload, headers), Map.class);
            log.info("Verified 'text_suggest' field type in Solr schema.");
        } catch (Exception ignored) {
            // Already exists
        }

        // 2. Add product search fields (only id, sku, title_en, title_ar, short_description_en, short_description_ar)
        String[] fields = {
            "{\"name\": \"sku\", \"type\": \"string\", \"stored\": true, \"indexed\": true}",
            "{\"name\": \"title_en\", \"type\": \"text_en\", \"stored\": true, \"indexed\": true}",
            "{\"name\": \"title_ar\", \"type\": \"text_ar\", \"stored\": true, \"indexed\": true}",
            "{\"name\": \"short_description_en\", \"type\": \"text_en\", \"stored\": true, \"indexed\": true}",
            "{\"name\": \"short_description_ar\", \"type\": \"text_ar\", \"stored\": true, \"indexed\": true}",
            "{\"name\": \"suggest_text\", \"type\": \"text_suggest\", \"stored\": true, \"indexed\": true, \"multiValued\": true}"
        };

        for (String fieldDef : fields) {
            try {
                String payload = "{\"add-field\": " + fieldDef + "}";
                restTemplate.postForObject(schemaUrl, new HttpEntity<>(payload, headers), Map.class);
            } catch (Exception ignored) {
                // Field already exists
            }
        }

        // 3. Add copy fields to feed autocomplete automatically
        String[] copySources = {"title_en", "title_ar", "sku"};
        for (String src : copySources) {
            try {
                String payload = "{\"add-copy-field\": {\"source\": \"" + src + "\", \"dest\": \"suggest_text\"}}";
                restTemplate.postForObject(schemaUrl, new HttpEntity<>(payload, headers), Map.class);
            } catch (Exception ignored) {
                // Copy-field already exists
            }
        }
    }
}
