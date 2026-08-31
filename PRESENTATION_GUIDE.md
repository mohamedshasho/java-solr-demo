# Apache Solr 10 & Spring Boot Bilingual Search Demo — Presentation Guide

Welcome to the **Apache Solr 10 + Spring Boot Bilingual E-Commerce Search Demo**. This guide provides an end-to-end presentation walkthrough, explaining key search architecture concepts, Lucene tokenizers/analyzers, eDisMax ranking, and the curated "Hard Search Test Cases".

---

## 1. Executive Summary & Architecture

### Why Solr + Relational DB (Dual-Tier Architecture)?
* **Relational Database (H2/PostgreSQL/MySQL)**: Single source of truth (Master Store). Holds all transactional, relational, and commercial data (prices, inventory, detailed multi-lingual descriptions, ratings, foreign keys, timestamps).
* **Apache Solr 10 (Search Engine Index)**: Specialized, lean Inverted Index. Stores only search-critical fields (`id`, `sku`, `title_en`, `title_ar`, `short_description_en`, `short_description_ar`).
* **Performance Gain**: Solr executes full-text searches, fuzzy matching, and multi-field boosting across +5,000 products in **under 3 milliseconds**.

```
+-----------------------------------------------------------------------------------+
|                               Presentation Dashboard                              |
|   (Bilingual Search, Typeahead Autocomplete, Highlighting, Hard Case Presets)     |
+-----------------------------------------+-----------------------------------------+
                                          | REST API
                                          v
+-----------------------------------------------------------------------------------+
|                                Spring Boot 4 Backend                              |
|                                                                                   |
|  [SearchController]        [DataSeederService]        [SolrSearchService]         |
|  - /api/products/search    - Seeds 5000+ bilingual    - eDisMax Query Parser      |
|  - /api/products/suggest     products to DB & Solr    - Field Weight Boosts       |
|  - /api/products/spellcheck - Batch indexing chunks   - Highlighter (<mark>)      |
+---------------------+-------------------------------------+-----------------------+
                      |                                     |
                      v                                     v
+--------------------------------------+   +----------------------------------------+
|        Apache Solr 10 Core           |   |       Relational Database (H2)         |
|  - Inverted Index (Sub-3ms queries)  |   |  - Master Data Store                   |
|  - text_en & text_ar Analyzers       |   |  - 20+ columns (prices, stock, CLOBs)  |
|  - Edge-NGram Autocomplete Index     |   |  - Web Console: /h2-console            |
+--------------------------------------+   +----------------------------------------+
```

---

## 2. Apache Solr Schema Design

As per specifications, the Solr document contains **exclusively** the 6 search-critical attributes:

| Solr Field | Type | Analyzer / Capabilities |
| :--- | :--- | :--- |
| `id` | `string` | Primary Key, exact match, used for joining with DB |
| `sku` | `string` | Stock Keeping Unit, exact & prefix lookup |
| `title_en` | `text_en` | Standard Tokenizer, Lowercase, Porter Stemmer, English Stopwords |
| `title_ar` | `text_ar` | Standard Tokenizer, Lowercase, Arabic Normalization, Arabic Stemmer, Stopwords |
| `short_description_en` | `text_en` | English full-text search with stemming |
| `short_description_ar` | `text_ar` | Arabic full-text search with normalization & stemming |

---

## 3. Explaining the 10 Hard Search Test Cases to Your Audience

| # | Test Case | Query | What Solr Does | Why SQL `LIKE '%...%'` Fails |
|---|---|---|---|---|
| **1** | **Arabic Alef Normalization** | `اجهزة ابل` | Lucene's `ArabicNormalizationFilter` folds all Alef variants (أ, إ, آ) into bare (ا). Finds "أجهزة ابل". | SQL treats `أ` and `ا` as completely different byte codes and returns 0 matches. |
| **2** | **Arabic Taa Marbuta / Haa** | `قهوه اسبريسو` | Normalizes `ة` and `ه` interchangeably. Finds "قهوة اسبريسو". | SQL fails if the user types `قهوه` and DB stores `قهوة`. |
| **3** | **Arabic Definite Article (الـ)** | `الاسبريسو` | `ArabicStemFilter` strips the prefix `الـ` from indexed tokens and search tokens. | SQL `LIKE '%الاسبريسو%'` misses products where title is only `اسبريسو`. |
| **4** | **Arabic Plural / Singular Stemming**| `سماعات` | Stemmer maps plural `سماعات` and singular `سماعة` to the common root. | SQL misses singular forms when searching plural. |
| **5** | **English Porter Stemmer (Plurals)** | `wireless headphones` | Stems `headphones` to `headphon`. Matches both singular and plural. | SQL misses singular `wireless headphone`. |
| **6** | **English Verb & Gerund Inflexions** | `running shoes` | Porter stemmer stems `running` and `run` to base root `run`. | SQL misses `run shoes` or `runner shoes`. |
| **7** | **Typo / Fuzzy Search (Brand)** | `samsng galxy` | Levenshtein Damerau edit distance (`~1` / `~2`) corrects typos and finds `Samsung Galaxy`. | SQL returns 0 matches for typo strings. |
| **8** | **Common Typo Variations** | `laptob gaming` | Matches `laptop` despite acoustic phonetic typo (`b` instead of `p`). | SQL returns 0 matches. |
| **9** | **SKU Prefix Match** | `ELEC-SAM` | Instant B-Tree prefix search on Solr string index in &lt; 1ms. | SQL full table scan on 5,000+ records is slow. |
| **10**| **Cross-Lingual Bilingual Queries** | `Samsung جالكسي` | `eDisMax` evaluates English and Arabic fields simultaneously with weighted scoring. | SQL requires expensive multi-column `OR` queries. |

---

## 4. REST API Endpoint Reference

### 1. Multi-Field Search
```bash
GET /api/products/search?q=samsung&page=0&size=10&fuzzy=false&highlight=true&lang=all&sortBy=score&op=AND
```
* **Parameters**:
  * `q`: Search keyword (English, Arabic, SKU, or mixed)
  * `page`, `size`: Pagination controls
  * `op`: `AND` (default, all terms must match) or `OR` (any term matches)
  * `fuzzy`: `true`/`false` (Levenshtein typo tolerance)
  * `highlight`: `true`/`false` (wraps matches in `<mark class="highlight-term">`)
  * `lang`: `all`, `en`, `ar`
  * `sortBy`: `score`, `price_asc`, `price_desc`

### 2. Autocomplete / Suggest (Typeahead)
```bash
GET /api/products/suggest?q=sam&limit=10
```
Returns fast prefix suggestions across English titles, Arabic titles, and SKUs.

### 3. Spellcheck / "Did you mean?"
```bash
GET /api/products/spellcheck?q=samsng
```
Returns spelling correction suggestions and collations.

### 4. Database CRUD & Solr Sync
* `GET /api/products`: Paginated database products
* `GET /api/products/{id}`: Detailed product record
* `POST /api/products`: Create product (saves to DB and indexes in Solr)
* `PUT /api/products/{id}`: Update product (updates DB and Solr)
* `DELETE /api/products/{id}`: Delete product (removes from DB and Solr)

### 5. Seeding, Re-indexing & Real-time Refreshing
* `POST /api/products/refresh`: Explicitly commits and opens a new Solr searcher, refreshing search results in real time.
* `POST /api/products/seed?count=5000`: Generates and indexes 5,000+ bilingual products.
* `POST /api/products/reindex`: Rebuilds entire Solr index from the relational database.
* `GET /api/demo/hard-examples`: Retrieves the 10 curated hard search test presets.
* `GET /api/demo/stats`: System metrics (DB record count, Solr indexed doc count).

---

## 5. Live Presentation Demo Steps

1. **Start the Application**:
   Open terminal and run:
   ```bash
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
   ./mvnw spring-boot:run
   ```
2. **Open the Presentation UI**:
   Navigate to `http://localhost:8080` in your web browser.
3. **Showcase System Metrics**:
   Point out the header indicators: Solr connection status, 5,000+ database records, and 5,000+ Solr indexed documents.
4. **Test Hard Search Presets**:
   Click on the cards in the **Hard Search Test Cases** section (e.g. *Arabic Alef Normalization*, *Typo Tolerance*, *English Stemming*). Show the audience how Solr finds the exact items in **< 3 milliseconds** and highlights the matching keywords.
5. **Inspect the Solr Query**:
   Click **"Toggle Solr Query Inspector"** to show the audience the exact `edismax` query, field weights (`title_en^4.0 title_ar^4.0 sku^6.0`), and highlighting parameters generated under the hood.
6. **Demonstrate Typeahead Autocomplete**:
   Type `sam` or `سما` in the search bar and show the instant dropdown suggestions.
7. **Demonstrate the Dual-Tier Modal**:
   Click **"Full DB Record"** on any card to show how Solr's lean search result seamlessly pulls the rich, multi-attribute record from the database.



## 6. Quick Test Commands
```
# Search with default AND operator
curl -s "http://localhost:8080/api/products/search?q=Samsung+Galaxy&op=AND"

# Search with OR operator
curl -s "http://localhost:8080/api/products/search?q=Samsung+Apple&op=OR"

# Real-time Solr Searcher Refresh
curl -X POST "http://localhost:8080/api/products/refresh"
```