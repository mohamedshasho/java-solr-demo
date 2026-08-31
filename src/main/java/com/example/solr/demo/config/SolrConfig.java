package com.example.solr.demo.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration class for Apache Solr 10 integration.
 * <p>
 * This class configures the connection between Spring Boot and Apache Solr using SolrJ 10's
 * modern JDK HTTP Client implementation (HttpJdkSolrClient), which leverages Java 21's native
 * HttpClient for high performance, virtual-thread readiness, and connection multiplexing.
 */
@Configuration
public class SolrConfig {

    @Value("${solr.base-url:http://localhost:8983/solr}")
    private String solrBaseUrl;

    @Value("${solr.core-name:products}")
    private String solrCoreName;

    /**
     * Creates a singleton {@link SolrClient} bean pointing to the Solr instance.
     *
     * @return SolrClient instance
     */
    @Bean
    public SolrClient solrClient() {
        return new HttpJdkSolrClient.Builder(solrBaseUrl)
                .withConnectionTimeout(15000, TimeUnit.MILLISECONDS)
                .withRequestTimeout(30000, TimeUnit.MILLISECONDS)
                .build();
    }

    public String getSolrBaseUrl() {
        return solrBaseUrl;
    }

    public String getSolrCoreName() {
        return solrCoreName;
    }
}
