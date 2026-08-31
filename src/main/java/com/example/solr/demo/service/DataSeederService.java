package com.example.solr.demo.service;

import com.example.solr.demo.config.SolrConfig;
import com.example.solr.demo.document.ProductSolrDoc;
import com.example.solr.demo.entity.Product;
import com.example.solr.demo.repository.ProductRepository;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for generating and seeding 5000+ realistic bilingual products
 * into both the Relational Database (H2) and Apache Solr Core.
 * <p>
 * Presentation Context:
 * ---------------------
 * High data volume (+5,000 products) is essential for demonstrating Solr's index speed,
 * inverted index efficiency, memory footprint, and sub-millisecond query execution
 * compared to relational SQL LIKE '%...%' scans.
 */
@Service
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final ProductRepository productRepository;
    private final SolrClient solrClient;
    private final SolrConfig solrConfig;

    @Value("${app.seeder.auto-seed-on-startup:true}")
    private boolean autoSeedOnStartup;

    @Value("${app.seeder.target-count:5000}")
    private int targetCount;

    @Value("${app.seeder.batch-size:500}")
    private int batchSize;

    public DataSeederService(ProductRepository productRepository, SolrClient solrClient, SolrConfig solrConfig) {
        this.productRepository = productRepository;
        this.solrClient = solrClient;
        this.solrConfig = solrConfig;
    }

    /**
     * Seeds initial 5,000+ products on startup if the database is currently empty.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(2) // Runs after SolrSchemaService (Order 1 / default)
    public void onStartup() {
        if (!autoSeedOnStartup) {
            log.info("Auto-seeding is disabled in application.properties.");
            return;
        }

        long existingCount = productRepository.count();
        if (existingCount < targetCount) {
            log.info("Current DB product count ({}) is less than target ({}). Starting automatic seed...", existingCount, targetCount);
            seedProducts(targetCount);
        } else {
            log.info("Database already contains {} products. Skipping startup seed.", existingCount);
        }
    }

    /**
     * Generates and bulk-inserts N bilingual products into Database and Apache Solr.
     *
     * @param count Number of products to generate (e.g., 5000)
     * @return Total seeded count
     */
    @Transactional
    public long seedProducts(int count) {
        String coreName = solrConfig.getSolrCoreName();
        log.info("Starting generation of {} bilingual products...", count);
        long startTime = System.currentTimeMillis();

        List<ProductTemplate> templates = buildProductTemplates();
        Random random = new Random(42); // Deterministic seed for reproducible demo data

        List<Product> batchToSave = new ArrayList<>(batchSize);
        long seededTotal = 0;

        // 1. First add curated anchor products specifically designed for presentation search test queries
        List<Product> anchorProducts = buildAnchorProducts();
        productRepository.saveAll(anchorProducts);
        indexBatchInSolr(coreName, anchorProducts);
        seededTotal += anchorProducts.size();

        // 2. Procedurally generate the remaining catalog
        int generatedCount = count - anchorProducts.size();
        for (int i = 1; i <= generatedCount; i++) {
            ProductTemplate tpl = templates.get(i % templates.size());
            Product product = generateProductFromTemplate(tpl, i, random);
            batchToSave.add(product);

            if (batchToSave.size() >= batchSize || i == generatedCount) {
                // Save batch to Relational Database
                List<Product> savedBatch = productRepository.saveAll(batchToSave);
                productRepository.flush();

                // Index batch into Apache Solr
                indexBatchInSolr(coreName, savedBatch);

                seededTotal += savedBatch.size();
                batchToSave.clear();
                log.info("Seeded and indexed {} / {} products...", seededTotal, count);
            }
        }

        // Commit Solr index
        try {
            solrClient.commit(coreName);
        } catch (Exception e) {
            log.error("Error committing Solr index: {}", e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Successfully seeded {} products in {} ms ({} sec) into DB and Solr core '{}'.",
                seededTotal, duration, duration / 1000.0, coreName);

        return seededTotal;
    }

    private void indexBatchInSolr(String coreName, List<Product> products) {
        try {
            List<ProductSolrDoc> solrDocs = new ArrayList<>(products.size());
            for (Product p : products) {
                solrDocs.add(new ProductSolrDoc(
                        String.valueOf(p.getId()),
                        p.getSku(),
                        p.getTitleEn(),
                        p.getTitleAr(),
                        p.getShortDescriptionEn(),
                        p.getShortDescriptionAr()
                ));
            }
            solrClient.addBeans(coreName, solrDocs);
        } catch (Exception e) {
            log.error("Failed to index batch in Solr: {}", e.getMessage());
        }
    }

    /**
     * Curated anchor products designed to illustrate search test cases in live demos.
     */
    private List<Product> buildAnchorProducts() {
        List<Product> list = new ArrayList<>();

        // 1. Samsung Smartphone (Typo test: "samsng", Arabic normalization: "سامسونج")
        list.add(createAnchor("ELEC-SAM-S24U-001",
                "Samsung Galaxy S24 Ultra 5G AI Smartphone 512GB Titanium Gray",
                "هاتف سامسونج جالاكسي اس 24 الترا الذكي مع دعم الذكاء الاصطناعي",
                "Flagship Samsung smartphone featuring 200MP camera, Snapdragon 8 Gen 3 processor, and built-in S Pen.",
                "أقوى هواتف سامسونج الرائدة مع كاميرا بدقة 200 ميجابكسل ومعالج سنابدراجون الجيل الثالث وقلم اس بين المدمج.",
                "Full specifications: 6.8-inch Dynamic AMOLED 2X display, 5000mAh battery, 45W fast charging.",
                "المواصفات الكاملة: شاشة اموليد مقاس 6.8 بوصة، بطارية 5000 مللي امبير، شحن سريع بقدرة 45 واط.",
                "Smartphones", "هواتف ذكية", "Samsung", new BigDecimal("4999.00"), 45, 4.9, 1280,
                "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=500", "smartphone,samsung,galaxy,flagship"));

        // 2. Apple iPhone (Arabic Hamza / stemmer test: "اجهزة", "أجهزة", "هواتف")
        list.add(createAnchor("ELEC-APPL-IP16P-002",
                "Apple iPhone 16 Pro Max 256GB Natural Titanium",
                "هاتف ابل ايفون 16 برو ماكس سعة 256 جيجابايت تيتانيوم طبيعي",
                "Latest Apple iPhone with A18 Pro chip, Camera Control button, and titanium aerospace design.",
                "أحدث أجهزة ابل الذكية مع شريحة إيه 18 برو وزر التحكم بالكاميرا وتصميم التيتانيوم الفاخر.",
                "Featuring 48MP Fusion camera system, Action button, USB-C 3.0 speeds, and ProMotion 120Hz display.",
                "يتميز بنظام كاميرا فيوجن 48 ميجابكسل وزر الإجراءات وشاشة بروموشن بتردد 120 هرتز.",
                "Smartphones", "هواتف ذكية", "Apple", new BigDecimal("5499.00"), 30, 4.8, 2150,
                "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=500", "iphone,apple,ios,mobile"));

        // 3. Sony Headphones (English plural/stemming: "headphones" vs "headphone")
        list.add(createAnchor("AUD-SONY-WH1000-003",
                "Sony WH-1000XM5 Wireless Noise Cancelling Over-Ear Headphones",
                "سماعات سوني اللاسلكية العازلة للضوضاء فوق الأذن احترافية",
                "Industry-leading active noise cancelling headphones with 30-hour battery life and crystal clear hands-free calling.",
                "أفضل سماعة عازلة للضوضاء في العالم مع بطارية تدوم حتى 30 ساعة وميكروفونات متعددة للمكالمات النقية.",
                "Engineered with two processors and eight microphones for unprecedented noise cancellation.",
                "مصممة بمعالجين مخصصين وثمانية ميكروفونات لتوفير تجربة عزل ضوضاء لا مثيل لها.",
                "Audio", "صوتيات وسماعات", "Sony", new BigDecimal("1399.00"), 60, 4.7, 850,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500", "audio,sony,headphones,wireless"));

        // 4. Apple AirPods (Arabic Plural / Singular test: "سماعات" vs "سماعة")
        list.add(createAnchor("AUD-APPL-AIRPOD-004",
                "Apple AirPods Pro 2nd Generation Wireless Earbuds with MagSafe Case",
                "سماعة ابل ايربودز برو الجيل الثاني اللاسلكية مع علبة ماج سيف",
                "True wireless earbuds with active noise cancellation, adaptive transparency, and personalized spatial audio.",
                "سماعات ابل اللاسلكية الذكية مع ميزة إلغاء الضوضاء النشط والصوت المكاني المخصص.",
                "Powered by Apple H2 headphone chip delivering richer bass and crystal-clear sound across all frequencies.",
                "مدعومة بشريحة اتش 2 المتطورة لتقديم صوت نقي وتجربة صوتية ثلاثية الأبعاد غامرة.",
                "Audio", "صوتيات وسماعات", "Apple", new BigDecimal("949.00"), 120, 4.8, 3400,
                "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=500", "apple,airpods,earbuds,audio"));

        // 5. Dell Laptop (Typo test: "laptob", "lap top")
        list.add(createAnchor("COMP-DELL-XPS15-005",
                "Dell XPS 15 9530 Intel Core i9 32GB RAM 1TB SSD Gaming Laptop OLED",
                "لابتوب ديل للألعاب والمعالجة المتطورة شاشة اوليد",
                "High performance laptop featuring Intel Core i9 processor, NVIDIA RTX 4070, and 3.5K OLED touchscreen display.",
                "كمبيوتر محمول فائق الأداء للألعاب والمونتاج مع معالج كور اي 9 وشاشة لمس اوليد فائقة الوضوح.",
                "Crafted with machined aluminum and carbon fiber palm rest for ultimate durability and premium aesthetics.",
                "هيكل متين من الألمنيوم المصقول وألياف الكربون لتوفير أعلى مستويات القوة والأناقة.",
                "Laptops", "أجهزة لابتوب", "Dell", new BigDecimal("8999.00"), 15, 4.6, 310,
                "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=500", "dell,laptop,gaming,xps,computer"));

        // 6. Nespresso Coffee Machine (Arabic normalization: "قهوه" vs "قهوة", "الاسبريسو" vs "اسبريسو")
        list.add(createAnchor("HOME-NESP-VERT-006",
                "Nespresso Vertuo Next Espresso and Premium Coffee Machine",
                "ماكينة قهوة اسبريسو نسبريسو فيرتو المتطورة لصنع القهوة المختصة",
                "Centrifusion technology coffee maker for brewing five cup sizes of rich barista espresso and drip coffee.",
                "صانعة القهوة الاسبريسو بتقنية الطرد المركزي لتحضير 5 أحجام مختلفة من القهوة اللذيذة برغوة كريمية.",
                "Connects via Bluetooth and Wi-Fi for automated firmware updates and optimal coffee capsule extraction.",
                "تتصل عبر البلوتوث والواي فاي لضمان أفضل استخلاص لكبسولات القهوة المختصة.",
                "Home Appliances", "أجهزة منزلية", "Nespresso", new BigDecimal("799.00"), 80, 4.7, 920,
                "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=500", "coffee,nespresso,espresso,kitchen"));

        // 7. Royal Arabic Oud Perfume (Arabic Diacritics & Terminology test)
        list.add(createAnchor("BEAU-OUD-ROYAL-007",
                "Royal Oud and Amber Arabic Luxury Perfume Eau De Parfum 100ml",
                "عطر العود الملكي والعنبر الفاخر عطر شرقي للجنسين مركز",
                "Enchanting oriental luxury fragrance with aged Cambodian agarwood, amber, damask rose, and white musk.",
                "عطر شرقي ملكي فاخر يمزج بين العود الكمبودي المعتق والعنبر والورد الجوري والمسك الأبيض الأصيل.",
                "Long-lasting sillage and concentrated projection suitable for special occasions and royal gatherings.",
                "ثبات ممتاز وفوحان قوي يناسب المناسبات الرسمية والاجتماعات الفاخرة.",
                "Perfumes", "عطور وتجميل", "Abdul Samad Al Qurashi", new BigDecimal("650.00"), 200, 4.9, 1500,
                "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=500", "perfume,oud,arabic,fragrance"));

        // 8. Nike Running Shoes (English Stemming: "running" vs "run" vs "runner")
        list.add(createAnchor("SPRT-NIKE-PEG40-008",
                "Nike Air Zoom Pegasus 40 Road Running Shoes For Men",
                "حذاء نايكي اير زوم للركض والرياضة اليومية مريح وخفيف",
                "A springy ride for any run with Nike React technology and two Zoom Air units for responsive cushioning.",
                "حذاء رياضي متطور للجري والتمارين مع تقنية نايكي رياكت لتوفير أقصى درجات الراحة والمرونة للقدمين.",
                "Engineered mesh upper offers lightweight breathability with optimized midfoot band for a secure fit.",
                "شبك علوي هندسي لتوفير تهوية مثالية ودعم قوي لوسط القدم أثناء الجري.",
                "Sports", "رياضة ولياقة", "Nike", new BigDecimal("549.00"), 150, 4.7, 680,
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500", "nike,running,shoes,sports"));

        // 9. Dyson Cordless Vacuum Cleaner
        list.add(createAnchor("HOME-DYSN-V15D-009",
                "Dyson V15 Detect Absolute Cordless Smart Vacuum Cleaner",
                "مكنسة دايسون اللاسلكية الذكية لتنظيف المنازل والأرضيات",
                "Intelligent cordless vacuum with laser illumination revealing invisible dust on hard floors and HEPA filtration.",
                "أحدث مكنسة لاسلكية ذكية مزودة بشعاع ليزر كاشف للغبار ونظام فلترة هيبا المتطور.",
                "Piezo sensor continuously calculates dust particles and increases suction power automatically when needed.",
                "حساس بيزو ذكي يحسب جزيئات الغبار ويزيد قوة الشفط تلقائياً حسب درجة الاتساخ.",
                "Home Appliances", "أجهزة منزلية", "Dyson", new BigDecimal("2899.00"), 35, 4.8, 740,
                "https://images.unsplash.com/photo-1558317374-067fb5f30001?w=500", "dyson,vacuum,home,cleaner"));

        // 10. PlayStation 5 Console
        list.add(createAnchor("GAME-SONY-PS5SL-010",
                "Sony PlayStation 5 Slim Console Digital Edition 1TB",
                "جهاز سوني بلايستيشن 5 سليم الإصدار الرقمي مساحة 1 تيرابايت",
                "Experience lightning-fast loading with an ultra-high speed SSD and deeper immersion with haptic feedback.",
                "عش تجربة الألعاب الخارقة مع جهاز بلايستيشن 5 سليم فائق السرعة مع دعم تقنية تتبع الأشعة.",
                "Slim design with 1TB of SSD storage built-in, 4K-TV gaming, up to 120fps with 120Hz output, and HDR technology.",
                "تصميم نحيف مدمج مع وحدة تخزين 1 تيرابايت ودعم دقة 4K ومعدل إطارات حتى 120 إطار في الثانية.",
                "Gaming", "ألعاب فيديو", "Sony", new BigDecimal("1849.00"), 90, 4.9, 4100,
                "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=500", "gaming,playstation,ps5,sony,console"));

        return list;
    }

    private Product createAnchor(String sku, String titleEn, String titleAr, String shortDescEn, String shortDescAr,
                                 String fullDescEn, String fullDescAr, String catEn, String catAr, String brand,
                                 BigDecimal price, int stock, double rating, int reviews, String img, String tags) {
        Product p = new Product();
        p.setSku(sku);
        p.setTitleEn(titleEn);
        p.setTitleAr(titleAr);
        p.setShortDescriptionEn(shortDescEn);
        p.setShortDescriptionAr(shortDescAr);
        p.setFullDescriptionEn(fullDescEn);
        p.setFullDescriptionAr(fullDescAr);
        p.setCategoryEn(catEn);
        p.setCategoryAr(catAr);
        p.setBrand(brand);
        p.setPrice(price);
        p.setCurrency("SAR");
        p.setStockQuantity(stock);
        p.setInStock(stock > 0);
        p.setRating(rating);
        p.setReviewsCount(reviews);
        p.setImageUrl(img);
        p.setTags(tags);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private Product generateProductFromTemplate(ProductTemplate tpl, int index, Random rnd) {
        Product p = new Product();

        String modifierEn = tpl.modifiersEn[index % tpl.modifiersEn.length];
        String modifierAr = tpl.modifiersAr[index % tpl.modifiersAr.length];
        String colorEn = COLORS_EN[index % COLORS_EN.length];
        String colorAr = COLORS_AR[index % COLORS_AR.length];
        String brand = tpl.brands[index % tpl.brands.length];

        String sku = String.format("%s-%s-%04d", tpl.skuPrefix, brand.substring(0, Math.min(3, brand.length())).toUpperCase(), index);
        p.setSku(sku);

        String titleEn = String.format("%s %s %s %s - %s", brand, tpl.baseTitleEn, modifierEn, colorEn, sku);
        String titleAr = String.format("%s %s %s %s باللون %s", tpl.baseTitleAr, brand, modifierAr, colorAr, colorAr);
        p.setTitleEn(titleEn);
        p.setTitleAr(titleAr);

        p.setShortDescriptionEn(String.format("Premium quality %s by %s with %s specifications. Engineered for superior performance and elegance.",
                tpl.baseTitleEn.toLowerCase(), brand, modifierEn.toLowerCase()));
        p.setShortDescriptionAr(String.format("%s فاخر ومميز من ماركة %s مع مواصفات %s المتطورة ومصنوع بأعلى معايير الجودة.",
                tpl.baseTitleAr, brand, modifierAr));

        p.setFullDescriptionEn(String.format("Detailed information for %s %s %s. Features comprehensive warranty, modern design, energy efficiency, and certified safety standards.",
                brand, tpl.baseTitleEn, modifierEn));
        p.setFullDescriptionAr(String.format("معلومات تفصيلية عن %s من %s. يتميز بضمان شامل وتصميم عصري وتوفير عالي للطاقة وأعلى معايير الأمان المعتمدة.",
                tpl.baseTitleAr, brand));

        p.setCategoryEn(tpl.categoryEn);
        p.setCategoryAr(tpl.categoryAr);
        p.setBrand(brand);

        double basePrice = tpl.basePrice + (rnd.nextDouble() * tpl.priceVariance);
        p.setPrice(BigDecimal.valueOf(basePrice).setScale(2, RoundingMode.HALF_UP));
        p.setCurrency("SAR");

        int stock = rnd.nextInt(250);
        p.setStockQuantity(stock);
        p.setInStock(stock > 0);

        double rating = 3.5 + (rnd.nextDouble() * 1.5);
        p.setRating(Math.round(rating * 10.0) / 10.0);
        p.setReviewsCount(10 + rnd.nextInt(990));

        p.setImageUrl(tpl.imageUrls[index % tpl.imageUrls.length]);
        p.setTags(String.format("%s,%s,%s,%s", tpl.categoryEn.toLowerCase(), brand.toLowerCase(), colorEn.toLowerCase(), modifierEn.toLowerCase()));

        p.setCreatedAt(LocalDateTime.now().minusDays(rnd.nextInt(365)));
        p.setUpdatedAt(LocalDateTime.now());

        return p;
    }

    private static final String[] COLORS_EN = {"Black", "White", "Silver", "Space Gray", "Navy Blue", "Emerald Green", "Rose Gold", "Midnight Blue", "Obsidian Black", "Titanium"};
    private static final String[] COLORS_AR = {"الأسود", "الأبيض", "الفضي", "الرمادي الفضائي", "الكحلي", "الأخضر الزمردي", "الذهبي الوردي", "الأزرق الليلي", "الأسود البركاني", "التيتانيوم"};

    private static class ProductTemplate {
        String skuPrefix;
        String categoryEn;
        String categoryAr;
        String baseTitleEn;
        String baseTitleAr;
        String[] brands;
        String[] modifiersEn;
        String[] modifiersAr;
        double basePrice;
        double priceVariance;
        String[] imageUrls;

        ProductTemplate(String skuPrefix, String categoryEn, String categoryAr, String baseTitleEn, String baseTitleAr,
                        String[] brands, String[] modifiersEn, String[] modifiersAr, double basePrice, double priceVariance, String[] imageUrls) {
            this.skuPrefix = skuPrefix;
            this.categoryEn = categoryEn;
            this.categoryAr = categoryAr;
            this.baseTitleEn = baseTitleEn;
            this.baseTitleAr = baseTitleAr;
            this.brands = brands;
            this.modifiersEn = modifiersEn;
            this.modifiersAr = modifiersAr;
            this.basePrice = basePrice;
            this.priceVariance = priceVariance;
            this.imageUrls = imageUrls;
        }
    }

    private List<ProductTemplate> buildProductTemplates() {
        List<ProductTemplate> list = new ArrayList<>();

        // 1. Smartphones & Tablets
        list.add(new ProductTemplate("SMART", "Smartphones", "هواتف ذكية وأجهزة لوحية",
                "Smartphone 5G OLED Display", "هاتف ذكي بشاشة اوليد الجيل الخامس",
                new String[]{"Samsung", "Apple", "Xiaomi", "Huawei", "Google Pixel", "OnePlus", "Oppo", "Honor"},
                new String[]{"Pro Max 256GB", "Ultra 512GB", "Plus 128GB", "Lite Edition", "Titanium Edition", "AI Special Edition"},
                new String[]{"برو ماكس 256 جيجابايت", "الترا 512 جيجابايت", "بلس 128 جيجابايت", "نسخة لايت", "إصدار التيتانيوم", "إصدار الذكاء الاصطناعي"},
                1200.0, 3500.0,
                new String[]{
                        "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500",
                        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500",
                        "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=500"
                }));

        // 2. Laptops & Computers
        list.add(new ProductTemplate("LAPT", "Laptops & Computers", "أجهزة كمبيوتر ولابتوب",
                "Laptop Intel Core SSD", "كمبيوتر محمول لابتوب بمعالج انتل وسرعة فائقة",
                new String[]{"Dell", "HP", "Lenovo", "Apple", "Asus", "Acer", "MSI", "Razer"},
                new String[]{"Gaming Edition RTX 4080", "Ultrabook 16GB RAM", "Workstation 32GB RAM", "Touchscreen 2-in-1", "Studio Edition"},
                new String[]{"إصدار الألعاب كارت شاشة خارق", "الترا بوك ذاكرة 16 جيجا", "محطة عمل احترافية 32 جيجا", "شاشة لمس 2 في 1", "إصدار استوديو للمصممين"},
                2200.0, 5000.0,
                new String[]{
                        "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=500",
                        "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500",
                        "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=500"
                }));

        // 3. Audio & Headphones
        list.add(new ProductTemplate("AUD", "Audio & Headphones", "سماعات وصوتيات",
                "Wireless Bluetooth Headphones", "سماعات رأس لاسلكية بلوتوث عازلة للضوضاء",
                new String[]{"Sony", "Bose", "Apple", "Sennheiser", "JBL", "Anker", "Beats", "Marshall"},
                new String[]{"Active Noise Cancelling", "Hi-Res Studio Sound", "True Wireless Earbuds", "Sport Waterproof", "Spatial Audio Edition"},
                new String[]{"مع ميزة إلغاء الضوضاء الفعال", "صوت استوديو فائق النقاء", "سماعة اذن لاسلكية مدمجة", "مقاومة للماء والتعرق للرياضة", "مع صوت محيطي ثلاثي الأبعاد"},
                250.0, 1200.0,
                new String[]{
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
                        "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=500",
                        "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500"
                }));

        // 4. Coffee & Espresso Machines
        list.add(new ProductTemplate("COFF", "Coffee & Espresso", "معدات ومكائن قهوة مختصة",
                "Automatic Espresso Coffee Machine", "ماكينة قهوة اسبريسو ومشروبات ساخنة أوتوماتيكية",
                new String[]{"Nespresso", "DeLonghi", "Breville", "Sage", "Philips", "Gaggia", "Jura", "Smeg"},
                new String[]{"15 Bar Pressure Barista", "Touch Screen Grinder", "Compact Capsule Brewer", "Double Boiler Professional", "Milk Frother System"},
                new String[]{"ضغط 15 بار بجودة الباريستا", "مطحنة مدمجة وشاشة لمس", "صانعة كبسولات مدمجة سريعة", "غلاية مزدوجة للمحترفين", "مع نظام تبخير الحليب التلقائي"},
                350.0, 2500.0,
                new String[]{
                        "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=500",
                        "https://images.unsplash.com/photo-1534432182912-63863115e106?w=500",
                        "https://images.unsplash.com/photo-1520970014086-2208d157c9e2?w=500"
                }));

        // 5. Luxury Perfumes & Fragrances
        list.add(new ProductTemplate("PERF", "Perfumes & Beauty", "عطور ومستحضرات تجميل فاخرة",
                "Luxury Eau De Parfum", "عطر شرقي فاخر ومركز للجنسين",
                new String[]{"Abdul Samad Al Qurashi", "Ajmal", "Tom Ford", "Chanel", "Dior", "Creed", "Amouage", "Rasasi"},
                new String[]{"Royal Cambodian Oud 100ml", "Amber and Velvet Rose", "Pure Musk and Jasmine", "Smoky Leather and Woods", "French Floral Essence"},
                new String[]{"بالعود الكمبودي الملكي 100 مل", "بالعنبر والورد الجوري الفاخر", "بالمسك النقي والياسمين", "برائحة الجلود المدخنة والأخشاب", "بخلاصة الزهور الفرنسية"},
                180.0, 950.0,
                new String[]{
                        "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=500",
                        "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=500",
                        "https://images.unsplash.com/photo-1547887537-6158d64c35b3?w=500"
                }));

        // 6. Home & Kitchen Appliances
        list.add(new ProductTemplate("HOME", "Home Appliances", "أجهزة منزلية ومطبخ",
                "Smart Home Appliance", "جهاز منزلي ذكي للمطبخ والراحة",
                new String[]{"Dyson", "Philips", "Ninja", "Tefal", "LG", "Samsung", "Bosch", "Panasonic"},
                new String[]{"Digital Air Fryer XXL", "Smart Robot Vacuum", "High Speed Blender", "Steam Iron System", "Induction Cooker"},
                new String[]{"قلاية هوائية رقمية حجم عائلي", "مكنسة روبوت ذكية ذاتية الشحن", "خلاط كهربائي فائق السرعة", "كواية بخار احترافية متطورة", "طباخ حثي كهربائي ذكي"},
                220.0, 1800.0,
                new String[]{
                        "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=500",
                        "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=500",
                        "https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=500"
                }));

        // 7. Gaming & Consoles
        list.add(new ProductTemplate("GAME", "Gaming", "ألعاب فيديو وإكسسوارات الألعاب",
                "Gaming Console & Accessories", "منصة ألعاب وإكسسوارات الجيمنج الاحترافية",
                new String[]{"Sony PlayStation", "Microsoft Xbox", "Nintendo", "Razer", "Logitech G", "Corsair", "SteelSeries", "HyperX"},
                new String[]{"Wireless Controller Pro", "Mechanical RGB Keyboard", "Ultra-Fast Gaming Mouse", "Virtual Reality VR Headset", "144Hz Curved Monitor"},
                new String[]{"يد تحكم لاسلكية احترافية", "كيبورد ميكانيكي مضيء بإضاءة ار جي بي", "ماوس ألعاب فائق السرعة والاستجابة", "نظارة واقع افتراضي ثلاثية الأبعاد", "شاشة ألعاب منحنية 144 هرتز"},
                150.0, 2200.0,
                new String[]{
                        "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=500",
                        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500",
                        "https://images.unsplash.com/photo-1526509867162-5b0c0d1b4b33?w=500"
                }));

        // 8. Sports & Fitness
        list.add(new ProductTemplate("SPRT", "Sports & Fitness", "رياضة ولياقة بدنية",
                "Athletic Running Shoes & Gear", "معدات وأحذية رياضية للركض واللياقة",
                new String[]{"Nike", "Adidas", "Puma", "Under Armour", "New Balance", "Asics", "Reebok", "Salomon"},
                new String[]{"Air Cushion Running Shoes", "Breathable Gym Apparel", "Smart Fitness Tracker Band", "Trail Running All-Terrain", "Carbon Plate Marathon"},
                new String[]{"حذاء ركض خفيف بوسادة هوائية", "ملابس رياضية مريحة تمتص العرق", "سوار تتبع اللياقة والنبض الذكي", "حذاء جري للطرق الوعرة والجبال", "حذاء ماراثون بكربون بليت متطور"},
                180.0, 850.0,
                new String[]{
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500",
                        "https://images.unsplash.com/photo-1556906781-9a412961c28c?w=500",
                        "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500"
                }));

        // 9. Smartwatches & Wearables
        list.add(new ProductTemplate("WATC", "Smartwatches", "ساعات ذكية وإلكترونية",
                "Smartwatch GPS Health Tracker", "ساعة ذكية لمتابعة الصحة والتمارين الرياضية",
                new String[]{"Apple Watch", "Samsung Galaxy Watch", "Garmin", "Huawei Watch", "Fitbit", "Amazfit", "Fossil", "Suunto"},
                new String[]{"Cellular Titanium 45mm", "Sapphire Glass Ultra", "Heart Rate & ECG Monitor", "Solar Charging Outdoor", "AMOLED Classic Leather"},
                new String[]{"إصدار شريحة اتصال تيتانيوم 45 ملم", "زجاج ياقوتي مقاوم للخدش الترا", "مستشعر تخطيط القلب ونبضات القلب", "شحن بالطاقة الشمسية للمغامرات", "شاشة اموليد مع سوار جلد طبيعي"},
                450.0, 2400.0,
                new String[]{
                        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500",
                        "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=500",
                        "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=500"
                }));

        // 10. Photography & Cameras
        list.add(new ProductTemplate("CAM", "Cameras & Photography", "كاميرات ومعدات تصوير احترافية",
                "Mirrorless Digital Camera 4K", "كاميرا تصوير رقمية احترافية بدون مرآة",
                new String[]{"Canon", "Sony Alpha", "Nikon", "Fujifilm", "Panasonic Lumix", "GoPro", "DJI", "Sigma"},
                new String[]{"Full Frame 45MP Cinema 4K", "Vlogging Creator Kit", "Action Camera Waterproof 5.3K", "Prime Lens 50mm f/1.2", "Gimbal 3-Axis Stabilizer"},
                new String[]{"إطار كامل 45 ميجابكسل سينمائية 4K", "باقة صناع المحتوى والفلوجات", "كاميرا حركة ومغامرات ضد الماء 5.3K", "عدسة تصوير بورتريه بفتحة 1.2", "مانع اهتزاز ومثبت ثلاثي المحاور"},
                1500.0, 6000.0,
                new String[]{
                        "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=500",
                        "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=500",
                        "https://images.unsplash.com/photo-1500646953400-045056a916d7?w=500"
                }));

        return list;
    }
}
