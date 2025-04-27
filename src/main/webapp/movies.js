import { verify_login } from './auth.js';
verify_login();

const searchForm = document.getElementById("search-form");
const resetButton = document.getElementById("reset-button");
const applySortButton = document.getElementById("apply-sort-button");

const sort1Field = document.getElementById("sort1-field");
const sort1Order = document.getElementById("sort1-order");
const sort2Field = document.getElementById("sort2-field");
const sort2Order = document.getElementById("sort2-order");

const resultsPerPageSelect = document.getElementById("results-per-page");
const prevButton = document.getElementById("prev-button");
const nextButton = document.getElementById("next-button");
const pageInfoSpan = document.getElementById("page-info");

function escapeHTML(str) {
    if (str === null || str === undefined) return 'N/A';
    return String(str).replace(/[&<>"'`=\/]/g, function (s) {
        return {
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
            '/': '&#x2F;', '`': '&#x60;', '=': '&#x3D;'
        }[s];
    });
}

function getUrlParams() {
    return new URLSearchParams(window.location.search);
}

function buildUpdatedUrl(newParams) {
    const urlParams = getUrlParams();
    const preservedParams = ['title', 'year', 'director', 'star_name', 'genre', 'titleInitial'];
    const currentFilters = {};
    preservedParams.forEach(param => {
        if (urlParams.has(param)) {
            currentFilters[param] = urlParams.get(param);
        }
    });

    const updatedUrlParams = new URLSearchParams();
    for (const key in currentFilters) {
        updatedUrlParams.set(key, currentFilters[key]);
    }

    for (const key in newParams) {
        if (newParams[key] !== null && newParams[key] !== undefined) {
            updatedUrlParams.set(key, newParams[key]);
        } else {
            updatedUrlParams.delete(key);
        }
    }

    if (!updatedUrlParams.has('page')) {
        updatedUrlParams.set('page', '1');
    }
    if (!updatedUrlParams.has('limit')) {
        const currentPagination = getCurrentPaginationParams(getUrlParams());
        updatedUrlParams.set('limit', currentPagination.limit);
    }
    if (!updatedUrlParams.has('sort1')) {
        const currentSort = getCurrentSortParams(getUrlParams());
        updatedUrlParams.set('sort1', currentSort.sort1);
        updatedUrlParams.set('order1', currentSort.order1);
        if (currentSort.sort2 && currentSort.sort2 !== 'none') {
            updatedUrlParams.set('sort2', currentSort.sort2);
            updatedUrlParams.set('order2', currentSort.order2);
        }
    }

    return updatedUrlParams.toString();
}


function getCurrentSortParams(urlParams) {
    return {
        sort1: urlParams.get('sort1') || 'rating',
        order1: urlParams.get('order1') || 'desc',
        sort2: urlParams.get('sort2') || 'title',
        order2: urlParams.get('order2') || 'asc'
    };
}

function getCurrentPaginationParams(urlParams) {
    const defaultLimit = 25;
    const limit = parseInt(urlParams.get('limit'), 10) || defaultLimit;
    const page = parseInt(urlParams.get('page'), 10) || 1;
    return {
        limit: [10, 25, 50, 100].includes(limit) ? limit : defaultLimit,
        page: page > 0 ? page : 1
    };
}

function updateSortControlsUI(urlParams) {
    const currentSort = getCurrentSortParams(urlParams);
    if(sort1Field) sort1Field.value = currentSort.sort1;
    if(sort1Order) sort1Order.value = currentSort.order1;
    if(sort2Field) sort2Field.value = (currentSort.sort2 && currentSort.sort2 !== 'none') ? currentSort.sort2 : 'none';
    if(sort2Order) {
        sort2Order.value = currentSort.order2;
        sort2Order.disabled = (sort2Field.value === 'none');
    }
}

function updatePaginationControlsUI(urlParams) {
    const currentPagination = getCurrentPaginationParams(urlParams);
    if(resultsPerPageSelect) resultsPerPageSelect.value = currentPagination.limit;
    if(pageInfoSpan) pageInfoSpan.textContent = `Page ${currentPagination.page}`;
}

function updateButtonStates(currentPage, hasMoreResults) {
    if(prevButton) prevButton.disabled = (currentPage <= 1);
    if(nextButton) nextButton.disabled = !hasMoreResults;
}

function addToCart(movieId) {
    console.log("Adding movie to cart:", movieId);
    jQuery.ajax({
        dataType: "json",
        method: "POST",
        url: "api/add-to-cart",
        data: {
            movieId: movieId
        },
        success: (resultData) => {
            console.log("Add to cart response:", resultData);
            if (resultData.status === "success") {
                const itemName = resultData.itemTitle ? resultData.itemTitle : `Movie ID ${resultData.itemId}`;
                alert("Added '" + escapeHTML(itemName) + "' to cart!");
            } else {
                alert("Failed to add item: " + (resultData.message || "Unknown error"));
            }
        },
        error: (jqXHR, textStatus, errorThrown) => {
            console.error("Add to cart error:", textStatus, errorThrown, jqXHR.responseText);
            let errorMsg = "Error adding item to cart.";
            if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
                errorMsg += " " + jqXHR.responseJSON.message;
            } else if (jqXHR.status) {
                errorMsg += ` Status: ${jqXHR.status}`;
            }
            alert(errorMsg);
        }
    });
}

function fetch_movies() {
    const moviesDetailsDiv = document.getElementById("movies-details");
    const urlParams = getUrlParams();

    if (!urlParams.has('sort1')) {
        urlParams.set('sort1', 'rating'); urlParams.set('order1', 'desc');
        urlParams.set('sort2', 'title'); urlParams.set('order2', 'asc');
    }
    if (!urlParams.has('limit')) { urlParams.set('limit', '25'); }
    if (!urlParams.has('page')) { urlParams.set('page', '1'); }

    let apiUrl = "api/movies?" + urlParams.toString();
    console.log("Fetching:", apiUrl);

    fetch(apiUrl)
        .then(response => {
            if (response.status === 401) {
                console.log("User not logged in, redirecting to login.");
                window.location.href = 'login.html';
                throw new Error("User not logged in.");
            }
            if (!response.ok) {
                return response.json().then(errData => {
                    throw new Error(`HTTP error! Status: ${response.status} - ${errData.error || errData.message || 'Unknown error'}`);
                }).catch((e) => {
                    throw new Error(`HTTP error! Status: ${response.status} ${response.statusText}. Server Response: ${e.message || 'No details'}`);
                });
            }
            return response.json();
        })
        .then(data => {
            console.log("Data received:", data);
            if (data.error) { throw new Error(`Server error: ${data.error} - ${data.detail || ''}`); }

            const tableBody = document.querySelector("#movies-table tbody");
            if (!tableBody) { console.error("Error: Could not find table body #movies-table tbody"); return; }
            tableBody.innerHTML = "";

            if (!data.movies || data.movies.length === 0) {
                console.log("No movies found in the response");
                const row = document.createElement("tr");
                const cell = document.createElement("td");
                cell.colSpan = 7;
                const genreFilter = urlParams.get('genre');
                const titleInitialFilter = urlParams.get('titleInitial');
                const searchTitle = urlParams.get('title');
                if (genreFilter) cell.textContent = `No movies found for genre "${escapeHTML(genreFilter)}"`;
                else if (titleInitialFilter) cell.textContent = `No movies found starting with "${escapeHTML(titleInitialFilter)}"`;
                else if (searchTitle) cell.textContent = `No movies found matching "${escapeHTML(searchTitle)}"`;
                else cell.textContent = "No movies found matching the criteria";
                row.appendChild(cell);
                tableBody.appendChild(row);
                updateButtonStates(data.currentPage || 1, false);
                if(pageInfoSpan) pageInfoSpan.textContent = `Page ${data.currentPage || 1}`;
            } else {
                data.movies.forEach(movie => {
                    const row = document.createElement("tr");
                    const genreLinks = (movie.genres || "")
                        .split(",").map(g => g.trim()).filter(g => g).slice(0, 3)
                        .map(trimmedGenre => `<a href="movies.html?${buildUpdatedUrl({ genre: trimmedGenre, page: 1 })}">${escapeHTML(trimmedGenre)}</a>`)
                        .join(", ");

                    const starLinks = (movie.stars || "")
                        .split(",").map(s => s.trim()).filter(s => s)
                        .map(starInfo => {
                            const parts = starInfo.split(":");
                            if (parts.length >= 2) {
                                const id = parts[0].trim(); const name = parts.slice(1).join(':').trim();
                                return `<a href="singlestar.html?id=${encodeURIComponent(id)}">${escapeHTML(name)}</a>`;
                            } return escapeHTML(starInfo);
                        }).join(", ");

                    const movieId = movie.id;
                    if (!movieId) { console.warn("Movie object missing 'id':", movie); }

                    row.innerHTML = `
                        <td><a href="singlemovie.html?id=${encodeURIComponent(movieId)}">${escapeHTML(movie.title)}</a></td>
                        <td>${escapeHTML(movie.year)}</td>
                        <td>${escapeHTML(movie.director)}</td>
                        <td>${genreLinks || 'N/A'}</td>
                        <td>${starLinks || 'N/A'}</td>
                        <td>${movie.rating !== null && movie.rating !== undefined && movie.rating !== 'N/A' ? escapeHTML(movie.rating) : 'N/A'}</td>
                        <td>
                            ${movieId ? `<button class="btn btn-primary btn-sm add-to-cart-btn" data-movie-id="${escapeHTML(movieId)}">Add to Cart</button>` : 'N/A'}
                        </td>
                    `;
                    tableBody.appendChild(row);
                });

                attachCartButtonListeners();

                updateButtonStates(data.currentPage, data.hasMoreResults);
                if(pageInfoSpan) pageInfoSpan.textContent = `Page ${data.currentPage}`;
            }

            if (moviesDetailsDiv) { moviesDetailsDiv.style.display = 'none'; }
        })
        .catch(error => {
            if (error.message === "User not logged in.") { return; }
            console.error("Error fetching or processing movies:", error);
            const tableBody = document.querySelector("#movies-table tbody");
            if (!tableBody) return;
            tableBody.innerHTML = "";
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 7;
            cell.textContent = "Error loading movies: " + error.message;
            cell.style.color = 'red';
            row.appendChild(cell);
            tableBody.appendChild(row);
            if (moviesDetailsDiv) { moviesDetailsDiv.style.display = 'none'; }
            if(prevButton) prevButton.disabled = true;
            if(nextButton) nextButton.disabled = true;
            if(pageInfoSpan) pageInfoSpan.textContent = 'Error';
        });
}

function attachCartButtonListeners() {
    const buttons = document.querySelectorAll('.add-to-cart-btn');
    buttons.forEach(button => {
        if (!button.dataset.listenerAttached) {
            button.addEventListener('click', function() {
                const movieId = this.dataset.movieId;
                if (movieId) {
                    addToCart(movieId);
                } else {
                    console.error('Movie ID missing from button data attribute');
                }
            });
            button.dataset.listenerAttached = 'true';
        }
    });
}

function fetchInitials() {
    fetch("api/title-initials")
        .then(response => response.json())
        .then(data => {
            const urlParams = getUrlParams();
            const selectedInitial = urlParams.get('titleInitial');
            const initialListDiv = document.querySelector("#initial-list div");
            if (!initialListDiv) return;
            initialListDiv.innerHTML = "";
            const currentPagination = getCurrentPaginationParams(urlParams);
            const currentSort = getCurrentSortParams(urlParams);

            data.initials.forEach(initial => {
                const link = document.createElement("a");
                const browseParams = {
                    titleInitial: initial, page: 1, limit: currentPagination.limit,
                    sort1: currentSort.sort1, order1: currentSort.order1,
                    sort2: currentSort.sort2, order2: currentSort.order2
                };
                link.href = "movies.html?" + buildUpdatedUrl(browseParams);
                link.textContent = initial;
                link.style.marginRight = "10px"; link.style.textDecoration = "none";
                link.style.color = "blue"; link.style.fontWeight = "bold";
                if (initial === selectedInitial) { link.classList.add("selected-browse-link"); link.style.color = "red"; }
                initialListDiv.appendChild(link);
            });
        })
        .catch(error => {
            console.error("Error fetching title initials:", error);
            const initialListDiv = document.querySelector("#initial-list div");
            if (initialListDiv) initialListDiv.textContent = "Failed to load title initials.";
        });
}

function fetchGenres() {
    fetch("api/genres")
        .then(response => response.json())
        .then(data => {
            const urlParams = getUrlParams();
            const selectedGenre = urlParams.get('genre');
            const genreListDiv = document.querySelector("#genre-list div");
            if (!genreListDiv) return;
            genreListDiv.innerHTML = "";
            const currentPagination = getCurrentPaginationParams(urlParams);
            const currentSort = getCurrentSortParams(urlParams);

            data.genres.forEach(genre => {
                const link = document.createElement("a");
                const browseParams = {
                    genre: genre, page: 1, limit: currentPagination.limit,
                    sort1: currentSort.sort1, order1: currentSort.order1,
                    sort2: currentSort.sort2, order2: currentSort.order2
                };
                link.href = "movies.html?" + buildUpdatedUrl(browseParams);
                link.textContent = genre;
                link.style.marginRight = "10px"; link.style.textDecoration = "none";
                link.style.color = "blue"; link.style.fontWeight = "bold";
                if (genre === selectedGenre) { link.classList.add("selected-browse-link"); link.style.color = "red"; }
                genreListDiv.appendChild(link);
            });
        })
        .catch(error => {
            console.error("Error fetching genres:", error);
            const genreListDiv = document.querySelector("#genre-list div");
            if (genreListDiv) genreListDiv.textContent = "Failed to load genres.";
        });
}

if(searchForm) {
    searchForm.addEventListener("submit", function(event) {
        event.preventDefault();
        const urlParams = getUrlParams();
        const currentSort = getCurrentSortParams(urlParams);
        const currentPagination = getCurrentPaginationParams(urlParams);
        const newParams = {};
        ['title', 'year', 'director', 'star_name'].forEach(param => {
            const element = document.getElementById(param);
            const value = element ? element.value.trim() : '';
            if (value) { newParams[param] = value; }
        });
        newParams.sort1 = currentSort.sort1; newParams.order1 = currentSort.order1;
        if (currentSort.sort2 && currentSort.sort2 !== 'none') {
            newParams.sort2 = currentSort.sort2; newParams.order2 = currentSort.order2;
        }
        newParams.limit = currentPagination.limit; newParams.page = '1';
        window.location.search = buildUpdatedUrl(newParams);
    });
}

if(resetButton) {
    resetButton.addEventListener("click", function() {
        if(searchForm) searchForm.reset();
        const urlParams = getUrlParams();
        const currentPagination = getCurrentPaginationParams(urlParams);
        const baseParams = { limit: currentPagination.limit, page: 1, sort1: 'rating', order1: 'desc', sort2: 'title', order2: 'asc' };
        window.location.search = buildUpdatedUrl(baseParams);
    });
}

if(applySortButton) {
    applySortButton.addEventListener("click", function() {
        const urlParams = getUrlParams();
        const currentPagination = getCurrentPaginationParams(urlParams);
        const s1f = sort1Field.value; const s1o = sort1Order.value;
        const s2f = sort2Field.value; const s2o = sort2Order.value;
        if (s2f !== 'none' && s1f === s2f) { alert("Primary and Secondary sort fields cannot be the same."); return; }
        const newParams = {};
        ['title', 'year', 'director', 'star_name', 'genre', 'titleInitial'].forEach(param => {
            if (urlParams.has(param)) { newParams[param] = urlParams.get(param); }
        });
        newParams.sort1 = s1f; newParams.order1 = s1o;
        if (s2f !== 'none') { newParams.sort2 = s2f; newParams.order2 = s2o; }
        else { newParams.sort2 = null; newParams.order2 = null; }
        newParams.limit = currentPagination.limit; newParams.page = '1';
        window.location.search = buildUpdatedUrl(newParams);
    });
}

if(resultsPerPageSelect) {
    resultsPerPageSelect.addEventListener('change', function() {
        const urlParams = getUrlParams();
        const newParams = {};
        ['title', 'year', 'director', 'star_name', 'genre', 'titleInitial', 'sort1', 'order1', 'sort2', 'order2'].forEach(param => {
            if (urlParams.has(param)) { newParams[param] = urlParams.get(param); }
        });
        newParams.limit = this.value; newParams.page = '1';
        window.location.search = buildUpdatedUrl(newParams);
    });
}

if(prevButton) {
    prevButton.addEventListener('click', function() {
        const urlParams = getUrlParams();
        const currentPagination = getCurrentPaginationParams(urlParams);
        if (currentPagination.page > 1) { urlParams.set('page', currentPagination.page - 1); window.location.search = urlParams.toString(); }
    });
}

if(nextButton) {
    nextButton.addEventListener('click', function() {
        const urlParams = getUrlParams();
        const currentPagination = getCurrentPaginationParams(urlParams);
        urlParams.set('page', currentPagination.page + 1); window.location.search = urlParams.toString();
    });
}

if(sort2Field) {
    sort2Field.addEventListener('change', function() {
        if(sort2Order) sort2Order.disabled = (this.value === 'none');
    });
}

document.addEventListener("DOMContentLoaded", function() {
    const urlParams = getUrlParams();
    updateSortControlsUI(urlParams);
    updatePaginationControlsUI(urlParams);
    ['title', 'year', 'director', 'star_name'].forEach(param => {
        if (urlParams.has(param)) {
            const element = document.getElementById(param);
            if (element) { element.value = urlParams.get(param); }
        }
    });
    fetchInitials();
    fetchGenres();
    fetch_movies();
});