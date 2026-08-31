// Apache Solr 10 Presentation Demo Client Application

let currentLang = 'all';
let currentOp = 'AND';
let currentPage = 0;
const pageSize = 12;
let debounceTimeout = null;
let currentSearchQuery = '';

document.addEventListener('DOMContentLoaded', () => {
    fetchStats();
    loadHardExamples();
    setupSearchInput();
    triggerSearch(); // Initial search showing top products
});

/**
 * Explicitly triggers Solr index searcher refresh and reloads stats & search results.
 */
async function refreshAll() {
    const icon = document.getElementById('refreshIcon');
    if (icon) icon.classList.add('fa-spin');

    try {
        await fetch('/api/products/refresh', { method: 'POST' });
        await fetchStats();
        await triggerSearch();
    } catch (e) {
        console.error('Refresh failed:', e);
    } finally {
        setTimeout(() => {
            if (icon) icon.classList.remove('fa-spin');
        }, 500);
    }
}

/**
 * Loads system statistics (DB product count, Solr indexed doc count).
 */
async function fetchStats() {
    try {
        const res = await fetch('/api/demo/stats');
        if (res.ok) {
            const data = await res.json();
            document.getElementById('statDbCount').innerText = Number(data.databaseProductCount).toLocaleString();
            document.getElementById('statSolrCount').innerText = Number(data.solrIndexedDocCount).toLocaleString();
            document.getElementById('modalDbCount').innerText = Number(data.databaseProductCount).toLocaleString();
            document.getElementById('modalSolrCount').innerText = Number(data.solrIndexedDocCount).toLocaleString();

            const indicator = document.getElementById('solrIndicator');
            const statusText = document.getElementById('solrStatusText');
            if (data.solrConnected) {
                indicator.className = 'w-2.5 h-2.5 rounded-full bg-emerald-400';
                statusText.innerText = `Solr Connected (${data.solrCoreName})`;
            } else {
                indicator.className = 'w-2.5 h-2.5 rounded-full bg-red-500';
                statusText.innerText = 'Solr Disconnected';
            }
        }
    } catch (e) {
        console.error('Failed to fetch stats:', e);
    }
}

/**
 * Fetches and renders curated Hard Search Test Cases for presentation.
 */
async function loadHardExamples() {
    try {
        const res = await fetch('/api/demo/hard-examples');
        if (res.ok) {
            const examples = await res.json();
            const grid = document.getElementById('hardExamplesGrid');
            grid.innerHTML = '';

            examples.forEach(ex => {
                const card = document.createElement('div');
                card.className = 'example-card bg-slate-800/80 hover:bg-slate-800 border border-slate-700/80 hover:border-amber-500/50 rounded-xl p-4 cursor-pointer flex flex-col justify-between';
                card.onclick = () => runHardExample(ex.query, ex.id);

                card.innerHTML = `
                    <div>
                        <div class="flex items-center justify-between gap-2 mb-2">
                            <span class="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-slate-700 text-slate-300">${ex.category}</span>
                            <span class="text-[10px] text-slate-400 font-mono">${ex.language}</span>
                        </div>
                        <h4 class="font-bold text-white text-sm mb-1">${ex.title}</h4>
                        <div class="flex items-center gap-2 bg-slate-900/90 rounded-lg px-2.5 py-1.5 border border-slate-700/60 mb-2">
                            <span class="text-xs text-slate-400">Query:</span>
                            <strong class="text-amber-400 font-mono text-xs ${ex.language === 'Arabic' ? 'arabic-text' : ''}">${ex.query}</strong>
                        </div>
                        <p class="text-xs text-slate-400 mb-2 leading-relaxed">${ex.solrMechanism}</p>
                    </div>
                    <div class="pt-2 border-t border-slate-700/50 flex items-center justify-between text-[11px]">
                        <span class="text-rose-400/90 font-medium"><i class="fa-solid fa-circle-xmark mr-1"></i>SQL Fails</span>
                        <span class="text-amber-400 font-semibold flex items-center gap-1">Run Test <i class="fa-solid fa-play text-[9px]"></i></span>
                    </div>
                `;
                grid.appendChild(card);
            });
        }
    } catch (e) {
        console.error('Failed to load hard examples:', e);
    }
}

/**
 * Runs a selected hard search example.
 */
function runHardExample(queryText, exampleId) {
    const input = document.getElementById('searchInput');
    input.value = queryText;
    currentPage = 0;

    // If typo example, automatically ensure fuzzy or standard search
    if (exampleId.includes('typo')) {
        document.getElementById('fuzzyToggle').checked = true;
    }

    triggerSearch();
    window.scrollTo({ top: 380, behavior: 'smooth' });
}

/**
 * Configures search input listeners and debounced autocomplete suggestions.
 */
function setupSearchInput() {
    const input = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchBtn');

    input.addEventListener('input', (e) => {
        const val = e.target.value;
        clearBtn.classList.toggle('hidden', !val);

        clearTimeout(debounceTimeout);
        if (val.trim().length >= 2) {
            debounceTimeout = setTimeout(() => fetchAutocomplete(val.trim()), 200);
        } else {
            hideAutocomplete();
        }
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            hideAutocomplete();
            currentPage = 0;
            triggerSearch();
        }
    });

    // Close autocomplete when clicking outside
    document.addEventListener('click', (e) => {
        if (!e.target.closest('#searchInput') && !e.target.closest('#autocompleteDropdown')) {
            hideAutocomplete();
        }
    });
}

/**
 * Fetches instant typeahead autocomplete suggestions from Solr.
 */
async function fetchAutocomplete(query) {
    try {
        const res = await fetch(`/api/products/suggest?q=${encodeURIComponent(query)}&limit=8`);
        if (res.ok) {
            const data = await res.json();
            renderAutocomplete(data.suggestions);
        }
    } catch (e) {
        console.error('Autocomplete failed:', e);
    }
}

function renderAutocomplete(suggestions) {
    const dropdown = document.getElementById('autocompleteDropdown');
    const list = document.getElementById('autocompleteList');

    if (!suggestions || suggestions.length === 0) {
        hideAutocomplete();
        return;
    }

    list.innerHTML = '';
    suggestions.forEach(item => {
        const row = document.createElement('div');
        row.className = 'px-4 py-2.5 hover:bg-slate-700/70 cursor-pointer flex items-center justify-between transition text-sm';
        row.onclick = () => {
            document.getElementById('searchInput').value = item.term;
            hideAutocomplete();
            currentPage = 0;
            triggerSearch();
        };

        row.innerHTML = `
            <div class="flex items-center space-x-2">
                <i class="fa-solid fa-magnifying-glass text-slate-400 text-xs"></i>
                <span class="text-slate-200 font-medium ${item.sourceField === 'title_ar' ? 'arabic-text' : ''}">${escapeHtml(item.term)}</span>
            </div>
            <span class="text-[10px] font-mono text-slate-400 bg-slate-900 px-2 py-0.5 rounded border border-slate-700">${item.type}</span>
        `;
        list.appendChild(row);
    });

    dropdown.classList.remove('hidden');
}

function hideAutocomplete() {
    document.getElementById('autocompleteDropdown').classList.add('hidden');
}

function clearSearch() {
    document.getElementById('searchInput').value = '';
    document.getElementById('clearSearchBtn').classList.add('hidden');
    hideAutocomplete();
    currentPage = 0;
    triggerSearch();
}

/**
 * Sets active language filter ('all', 'en', 'ar').
 */
function setLanguage(lang) {
    currentLang = lang;
    document.querySelectorAll('.lang-pill').forEach(btn => {
        btn.classList.remove('active-pill', 'border-transparent');
        btn.classList.add('border-slate-700', 'text-slate-400');
    });

    const activeBtn = document.getElementById(lang === 'all' ? 'langBtnAll' : (lang === 'en' ? 'langBtnEn' : 'langBtnAr'));
    if (activeBtn) {
        activeBtn.classList.add('active-pill');
        activeBtn.classList.remove('border-slate-700', 'text-slate-400');
    }

    currentPage = 0;
    triggerSearch();
}

/**
 * Sets active default query operator ('AND' or 'OR').
 */
function setOperator(op) {
    currentOp = op;
    document.querySelectorAll('.op-pill').forEach(btn => {
        btn.classList.remove('active-pill', 'border-transparent');
        btn.classList.add('border-slate-700', 'text-slate-400');
    });

    const activeBtn = document.getElementById(op === 'AND' ? 'opBtnAnd' : 'opBtnOr');
    if (activeBtn) {
        activeBtn.classList.add('active-pill');
        activeBtn.classList.remove('border-slate-700', 'text-slate-400');
    }

    currentPage = 0;
    triggerSearch();
}

/**
 * Triggers full multi-field Solr search.
 */
async function triggerSearch() {
    const q = document.getElementById('searchInput').value.trim();
    currentSearchQuery = q;
    const fuzzy = document.getElementById('fuzzyToggle').checked;
    const highlight = document.getElementById('highlightToggle').checked;
    const sortBy = document.getElementById('sortBySelect').value;

    const url = `/api/products/search?q=${encodeURIComponent(q)}&page=${currentPage}&size=${pageSize}&fuzzy=${fuzzy}&highlight=${highlight}&lang=${currentLang}&sortBy=${sortBy}&op=${currentOp}`;

    try {
        const res = await fetch(url);
        if (res.ok) {
            const data = await res.json();
            renderSearchResults(data);
        }
    } catch (e) {
        console.error('Search request failed:', e);
    }
}

/**
 * Renders search results and Solr metadata.
 */
function renderSearchResults(data) {
    const container = document.getElementById('resultsContainer');
    const emptyState = document.getElementById('emptyState');
    const metricsBar = document.getElementById('metricsBar');
    const didYouMeanBanner = document.getElementById('didYouMeanBanner');
    const solrQueryDisplay = document.getElementById('solrQueryDisplay');

    // Update Metrics
    metricsBar.classList.remove('hidden');
    document.getElementById('searchedQueryLabel').innerText = data.query || '*:*';
    document.getElementById('totalHitsBadge').innerText = data.totalHits.toLocaleString();
    document.getElementById('qTimeBadge').innerText = `${data.qTimeMs} ms`;
    solrQueryDisplay.innerText = data.parsedSolrQuery || 'N/A';

    // Spellcheck "Did you mean?"
    if (data.didYouMean && data.totalHits === 0) {
        document.getElementById('didYouMeanText').innerText = data.didYouMean;
        didYouMeanBanner.classList.remove('hidden');
    } else {
        didYouMeanBanner.classList.add('hidden');
    }

    // Empty state check
    if (!data.items || data.items.length === 0) {
        container.innerHTML = '';
        emptyState.classList.remove('hidden');
        document.getElementById('paginationContainer').classList.add('hidden');
        return;
    }

    emptyState.classList.add('hidden');
    container.innerHTML = '';

    // Render Product Cards
    data.items.forEach(item => {
        const card = document.createElement('div');
        card.className = 'bg-slate-800/90 border border-slate-700/80 hover:border-slate-600 rounded-2xl overflow-hidden shadow-xl hover:shadow-2xl transition flex flex-col justify-between';

        const titleDisplayEn = item.highlightedTitle || item.titleEn;
        const descDisplayEn = item.highlightedDescription || item.shortDescriptionEn;

        card.innerHTML = `
            <div class="p-5 flex-1">
                <!-- Header: SKU & Score Badge -->
                <div class="flex items-center justify-between gap-2 mb-3">
                    <span class="text-xs font-mono font-bold px-2 py-0.5 rounded bg-slate-900 border border-slate-700 text-amber-400">
                        ${escapeHtml(item.sku || 'N/A')}
                    </span>
                    <div class="flex items-center space-x-1.5 text-xs text-slate-400">
                        <i class="fa-solid fa-chart-line text-emerald-400"></i>
                        <span>Score: <strong class="text-slate-200 font-mono">${(item.score || 1.0).toFixed(2)}</strong></span>
                    </div>
                </div>

                <!-- English Title (with highlights) -->
                <h4 class="font-bold text-white text-base mb-1.5 leading-snug">
                    ${titleDisplayEn || 'Untitled'}
                </h4>

                <!-- Arabic Title -->
                <h5 class="arabic-text font-bold text-amber-300/90 text-sm mb-3">
                    ${item.titleAr || ''}
                </h5>

                <!-- Descriptions -->
                <div class="space-y-2 mb-4 text-xs">
                    <p class="text-slate-300 leading-relaxed">${descDisplayEn || ''}</p>
                    <p class="arabic-text text-slate-400 leading-relaxed">${item.shortDescriptionAr || ''}</p>
                </div>
            </div>

            <!-- Footer: Price, Stock & DB Modal Trigger -->
            <div class="px-5 py-3.5 bg-slate-900/80 border-t border-slate-700/60 flex items-center justify-between">
                <div>
                    <span class="text-xs text-slate-400">Price:</span>
                    <span class="text-base font-extrabold text-emerald-400 ml-1">
                        ${item.price ? Number(item.price).toFixed(2) + ' SAR' : 'N/A'}
                    </span>
                </div>
                <button
                    onclick="openProductModal('${item.id}')"
                    class="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-600 rounded-lg text-xs font-semibold text-sky-300 hover:text-sky-200 transition flex items-center space-x-1.5"
                >
                    <i class="fa-solid fa-circle-info"></i>
                    <span>Full DB Record</span>
                </button>
            </div>
        `;
        container.appendChild(card);
    });

    // Pagination
    renderPagination(data.page, data.totalPages);
}

function renderPagination(page, totalPages) {
    const container = document.getElementById('paginationContainer');
    if (totalPages <= 1) {
        container.classList.add('hidden');
        return;
    }
    container.classList.remove('hidden');

    document.getElementById('prevPageBtn').disabled = (page === 0);
    document.getElementById('nextPageBtn').disabled = (page >= totalPages - 1);
    document.getElementById('pageIndicator').innerText = `Page ${page + 1} of ${totalPages}`;
}

function changePage(delta) {
    currentPage += delta;
    triggerSearch();
    window.scrollTo({ top: 400, behavior: 'smooth' });
}

function applyDidYouMean() {
    const text = document.getElementById('didYouMeanText').innerText;
    document.getElementById('searchInput').value = text;
    currentPage = 0;
    triggerSearch();
}

function enableFuzzyAndSearch() {
    document.getElementById('fuzzyToggle').checked = true;
    currentPage = 0;
    triggerSearch();
}

function toggleSolrDebug() {
    const box = document.getElementById('solrDebugBox');
    box.classList.toggle('hidden');
}

/**
 * Fetches and displays the full relational database record for a product.
 */
async function openProductModal(productId) {
    const modal = document.getElementById('productDetailModal');
    const content = document.getElementById('productModalContent');

    content.innerHTML = '<div class="text-center py-10"><i class="fa-solid fa-spinner fa-spin text-2xl text-amber-400"></i><p class="mt-2 text-xs text-slate-400">Loading Database Record...</p></div>';
    modal.classList.remove('hidden');

    try {
        const res = await fetch(`/api/products/${productId}`);
        if (res.ok) {
            const p = await res.json();
            content.innerHTML = `
                <div class="flex items-center justify-between border-b border-slate-700 pb-3 mb-4">
                    <div>
                        <span class="text-xs font-mono text-amber-400 bg-slate-900 px-2 py-0.5 rounded border border-slate-700">${p.sku}</span>
                        <h3 class="text-lg font-extrabold text-white mt-1">Full Relational Database Record (ID: ${p.id})</h3>
                    </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4 text-xs">
                    <div class="bg-slate-900/90 p-3.5 rounded-xl border border-slate-700/80">
                        <span class="text-slate-400 font-semibold block mb-1">English Title:</span>
                        <p class="text-white font-medium mb-2">${escapeHtml(p.titleEn)}</p>
                        <span class="text-slate-400 font-semibold block mb-1">Full Description (English CLOB):</span>
                        <p class="text-slate-300 leading-relaxed">${escapeHtml(p.fullDescriptionEn || p.shortDescriptionEn)}</p>
                    </div>

                    <div class="bg-slate-900/90 p-3.5 rounded-xl border border-slate-700/80">
                        <span class="text-slate-400 font-semibold block mb-1">العنوان بالعربية:</span>
                        <p class="arabic-text text-amber-300 font-bold mb-2">${escapeHtml(p.titleAr)}</p>
                        <span class="text-slate-400 font-semibold block mb-1">الوصف التفصيلي (Arabic CLOB):</span>
                        <p class="arabic-text text-slate-300 leading-relaxed">${escapeHtml(p.fullDescriptionAr || p.shortDescriptionAr)}</p>
                    </div>
                </div>

                <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-900/60 p-3.5 rounded-xl border border-slate-700/60 text-xs mb-4">
                    <div>
                        <span class="text-slate-400 block">Brand:</span>
                        <strong class="text-slate-200">${p.brand || 'N/A'}</strong>
                    </div>
                    <div>
                        <span class="text-slate-400 block">Price:</span>
                        <strong class="text-emerald-400">${p.price} ${p.currency}</strong>
                    </div>
                    <div>
                        <span class="text-slate-400 block">Stock Level:</span>
                        <strong class="${p.stockQuantity > 0 ? 'text-emerald-400' : 'text-red-400'}">${p.stockQuantity} in stock</strong>
                    </div>
                    <div>
                        <span class="text-slate-400 block">Rating:</span>
                        <strong class="text-amber-400"><i class="fa-solid fa-star text-xs"></i> ${p.rating} (${p.reviewsCount} reviews)</strong>
                    </div>
                </div>

                <div class="text-[11px] text-slate-500 flex justify-between pt-2 border-t border-slate-700/60">
                    <span>Created: ${p.createdAt || 'N/A'}</span>
                    <span>Last Updated: ${p.updatedAt || 'N/A'}</span>
                </div>
            `;
        }
    } catch (e) {
        content.innerHTML = '<p class="text-rose-400 text-xs">Failed to load product details.</p>';
    }
}

function closeProductModal() {
    document.getElementById('productDetailModal').classList.add('hidden');
}

/**
 * Seeder and Re-indexing Modal actions
 */
function openSeederModal() {
    document.getElementById('seederModal').classList.remove('hidden');
    fetchStats();
}

function closeSeederModal() {
    document.getElementById('seederModal').classList.add('hidden');
}

async function triggerSeed(count) {
    const btn = document.getElementById('seed5kBtn');
    const msg = document.getElementById('seederStatusMessage');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> Generating & Indexing 5,000 Products...';
    msg.classList.remove('hidden');
    msg.className = 'p-3 rounded-lg text-xs bg-amber-500/20 text-amber-300 border border-amber-500/30';
    msg.innerText = 'Seeding in progress... Batch inserting into H2 and indexing into Solr core.';

    try {
        const res = await fetch(`/api/products/seed?count=${count}`, { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            msg.className = 'p-3 rounded-lg text-xs bg-emerald-500/20 text-emerald-300 border border-emerald-500/30';
            msg.innerText = `Success: ${data.message} Total Seeded: ${data.totalSeeded}`;
            fetchStats();
            triggerSearch();
        }
    } catch (e) {
        msg.className = 'p-3 rounded-lg text-xs bg-red-500/20 text-red-300 border border-red-500/30';
        msg.innerText = 'Failed to seed products: ' + e.message;
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-plus-circle mr-2"></i> Generate & Index 5,000 Products';
    }
}

async function triggerReindex() {
    const btn = document.getElementById('reindexBtn');
    const msg = document.getElementById('seederStatusMessage');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> Re-indexing Solr...';
    msg.classList.remove('hidden');
    msg.className = 'p-3 rounded-lg text-xs bg-sky-500/20 text-sky-300 border border-sky-500/30';
    msg.innerText = 'Rebuilding Solr index from database...';

    try {
        const res = await fetch('/api/products/reindex', { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            msg.className = 'p-3 rounded-lg text-xs bg-emerald-500/20 text-emerald-300 border border-emerald-500/30';
            msg.innerText = `Success: ${data.message} Total Indexed: ${data.totalIndexed}`;
            fetchStats();
            triggerSearch();
        }
    } catch (e) {
        msg.className = 'p-3 rounded-lg text-xs bg-red-500/20 text-red-300 border border-red-500/30';
        msg.innerText = 'Re-index failed: ' + e.message;
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-rotate mr-2"></i> Full Solr Core Re-index';
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[m]);
}
