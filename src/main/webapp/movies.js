import { verify_login } from './auth.js';
verify_login();

const searchForm = document.getElementById("search-form");
const resetButton = document.getElementById("reset-button");
const applySortButton = document.getElementById("apply-sort-button");

const sort1Field = document.getElementById("sort1-field");
const sort1Order = document.getElementById("sort1-order");
const sort2Field = document.getElementById("sort2-field");
const sort2Order = document.getElementById("sort2-order");

function getCurrentSortParams() {
    const urlParams = new URLSearchParams(window.location.search);
    return {
        sort1: urlParams.get('sort1') || 'rating',
        order1: urlParams.get('order1') || 'desc',
        sort2: urlParams.get('sort2') || 'title',
        order2: urlParams.get('order2') || 'asc'
    };
}

function updateSortControlsUI() {
    const currentSort = getCurrentSortParams();
    sort1Field.value = currentSort.sort1;
    sort1Order.value = currentSort.order1;
    sort2Field.value = (currentSort.sort2 && currentSort.sort2 !== 'none') ? currentSort.sort2 : 'none';
    sort2Order.value = currentSort.order2;
    sort2Order.disabled = (sort2Field.value === 'none');
}


searchForm.addEventListener("submit", function(event) {
    event.preventDefault();
    const urlParams = new URLSearchParams(window.location.search);
    const currentSort = getCurrentSortParams();

    ['title', 'year', 'director', 'star_name'].forEach(param => {
        const value = document.getElementById(param).value.trim();
        if (value) {
            urlParams.set(param, value);
        } else {
            urlParams.delete(param);
        }
    });

    urlParams.set('sort1', currentSort.sort1);
    urlParams.set('order1', currentSort.order1);
    if (currentSort.sort2 && currentSort.sort2 !== 'none') {
        urlParams.set('sort2', currentSort.sort2);
        urlParams.set('order2', currentSort.order2);
    } else {
        urlParams.delete('sort2');
        urlParams.delete('order2');
    }

    window.location.search = urlParams.toString();
});

resetButton.addEventListener("click", function() {
    document.getElementById("search-form").reset();
    window.location.href = "movies.html";
});

applySortButton.addEventListener("click", function() {
    const urlParams = new URLSearchParams(window.location.search);

    const s1f = sort1Field.value;
    const s1o = sort1Order.value;
    const s2f = sort2Field.value;
    const s2o = sort2Order.value;

    if (s2f !== 'none' && s1f === s2f) {
        alert("Primary and Secondary sort fields cannot be the same.");
        return;
    }

    urlParams.set('sort1', s1f);
    urlParams.set('order1', s1o);

    if (s2f !== 'none') {
        urlParams.set('sort2', s2f);
        urlParams.set('order2', s2o);
    } else {
        urlParams.delete('sort2');
        urlParams.delete('order2');
    }

    window.location.search = urlParams.toString();
});

sort2Field.addEventListener('change', function() {
    sort2Order.disabled = (this.value === 'none');
});


document.addEventListener("DOMContentLoaded", function() {
    updateSortControlsUI();

    const urlParams = new URLSearchParams(window.location.search);
    ['title', 'year', 'director', 'star_name'].forEach(param => {
        if (urlParams.has(param)) {
            document.getElementById(param).value = urlParams.get(param);
        }
    });

    fetch_movies();
});


function fetch_movies() {
    const moviesDetailsDiv = document.getElementById("movies-details");
    const urlParams = new URLSearchParams(window.location.search);

    let apiUrl = "api/movies?" + urlParams.toString();
    console.log("Fetching:", apiUrl);

    fetch(apiUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log("Data received:", data);
            const tableBody = document.querySelector("#movies-table tbody");
            tableBody.innerHTML = "";

            if (!data.movies || data.movies.length === 0) {
                console.log("No movies found in the response");
                const row = document.createElement("tr");
                const cell = document.createElement("td");
                cell.colSpan = 6;
                const genreFilter = urlParams.get('genre');
                cell.textContent = genreFilter ? `No movies found for genre "${genreFilter}"` : "No movies found matching the criteria";
                row.appendChild(cell);
                tableBody.appendChild(row);
            } else {
                data.movies.forEach(movie => {
                    const row = document.createElement("tr");

                    const genreLinks = (movie.genres || "")
                        .split(",")
                        .map(genre => genre.trim())
                        .filter(genre => genre)
                        .slice(0, 3)
                        .map(trimmedGenre => {
                            const encodedGenre = encodeURIComponent(trimmedGenre);
                            return `<a href="movies.html?genre=${encodedGenre}">${trimmedGenre}</a>`;
                        })
                        .join(", ");

                    const starLinks = (movie.stars || "")
                        .split(",")
                        .map(star => {
                            const parts = star.split(":");
                            if (parts.length >= 2) {
                                const id = parts[0].trim();
                                const name = parts.slice(1).join(':').trim();
                                return `<a href="singlestar.html?id=${encodeURIComponent(id)}">${name}</a>`;
                            }
                            return star.trim();
                        })
                        .join(", ");


                    row.innerHTML = `
                        <td><a href="singlemovie.html?id=${encodeURIComponent(movie.id)}">${movie.title}</a></td>
                        <td>${movie.year}</td>
                        <td>${movie.director}</td>
                        <td>${genreLinks}</td>
                        <td>${starLinks}</td>
                        <td>${movie.rating !== null && movie.rating !== undefined ? movie.rating : 'N/A'}</td>
                    `;
                    tableBody.appendChild(row);
                });
            }

            if (moviesDetailsDiv) {
                moviesDetailsDiv.style.display = 'none';
            }

        })
        .catch(error => {
            console.error("Error fetching movies:", error);
            const tableBody = document.querySelector("#movies-table tbody");
            tableBody.innerHTML = "";

            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 6;
            cell.textContent = "Error loading movies: " + error.message;
            row.appendChild(cell);
            tableBody.appendChild(row);

            if (moviesDetailsDiv) {
                moviesDetailsDiv.style.display = 'none';
            }
        });
}