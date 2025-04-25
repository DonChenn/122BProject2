import { verify_login } from './auth.js';
verify_login();

let currentSort = {
    sort1: 'rating',
    order1: 'desc',
    sort2: 'title',
    order2: 'asc'
};

document.addEventListener("DOMContentLoaded", function() {
    updateSortLinks();
    fetch_movies();
});

function updateSortLinks() {
    const urlParams = new URLSearchParams(window.location.search);
    const sort1 = urlParams.get('sort1') || currentSort.sort1;
    const order1 = urlParams.get('order1') || currentSort.order1;
    const sort2 = urlParams.get('sort2') || currentSort.sort2;
    const order2 = urlParams.get('order2') || currentSort.order2;

    currentSort = { sort1, order1, sort2, order2 };

    const titleLink = document.getElementById('sort-title-link');
    const ratingLink = document.getElementById('sort-rating-link');
    const titleArrow = titleLink.querySelector('.arrow');
    const ratingArrow = ratingLink.querySelector('.arrow');

    titleArrow.textContent = '';
    ratingArrow.textContent = '';

    const existingParams = new URLSearchParams(window.location.search);
    const baseParams = new URLSearchParams();
    if (existingParams.has('genre')) {
        baseParams.set('genre', existingParams.get('genre'));
    }

    let titleParams = new URLSearchParams(baseParams);
    if (sort1 === 'title') {
        titleArrow.textContent = (order1 === 'asc') ? ' ▲' : ' ▼';
        titleParams.set('sort1', 'title');
        titleParams.set('order1', (order1 === 'asc') ? 'desc' : 'asc');
        titleParams.set('sort2', 'rating');
        titleParams.set('order2', 'desc');
    } else {
        titleParams.set('sort1', 'title');
        titleParams.set('order1', 'asc');
        titleParams.set('sort2', 'rating');
        titleParams.set('order2', 'desc');
    }
    titleLink.href = `movies.html?${titleParams.toString()}`;


    let ratingParams = new URLSearchParams(baseParams);
    if (sort1 === 'rating') {
        ratingArrow.textContent = (order1 === 'asc') ? ' ▲' : ' ▼';
        ratingParams.set('sort1', 'rating');
        ratingParams.set('order1', (order1 === 'asc') ? 'desc' : 'asc');
        ratingParams.set('sort2', 'title');
        ratingParams.set('order2', 'asc');
    } else {
        ratingParams.set('sort1', 'rating');
        ratingParams.set('order1', 'desc');
        ratingParams.set('sort2', 'title');
        ratingParams.set('order2', 'asc');
    }
    ratingLink.href = `movies.html?${ratingParams.toString()}`;
}


function fetch_movies() {
    const moviesDetailsDiv = document.getElementById("movies-details");

    const urlParams = new URLSearchParams(window.location.search);
    const genreFilter = urlParams.get('genre');
    const { sort1, order1, sort2, order2 } = currentSort;

    let apiParams = new URLSearchParams();
    if (genreFilter) {
        apiParams.set('genre', genreFilter);
    }
    apiParams.set('sort1', sort1);
    apiParams.set('order1', order1);
    apiParams.set('sort2', sort2);
    apiParams.set('order2', order2);

    let apiUrl = "api/movies?" + apiParams.toString();
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
                cell.textContent = genreFilter ? `No movies found for genre "${genreFilter}"` : "No movies found";
                row.appendChild(cell);
                tableBody.appendChild(row);
            } else {
                data.movies.forEach(movie => {
                    const row = document.createElement("tr");

                    const genreLinks = (movie.genres || "")
                        .split(",")
                        .slice(0, 3)
                        .map(genre => {
                            const trimmedGenre = genre.trim();
                            const encodedGenre = encodeURIComponent(trimmedGenre);
                            return `<a href="movies.html?genre=${encodedGenre}">${trimmedGenre}</a>`;
                        })
                        .join(", ");

                    const starLinks = (movie.stars || "")
                        .split(",")
                        .map(star => {
                            const parts = star.split(":");
                            const id = parts[0];
                            const name = parts.length > 1 ? parts[1].trim() : 'Unknown Star';
                            return `<a href="singlestar.html?id=${encodeURIComponent(id)}">${name}</a>`;
                        })
                        .join(", ");

                    row.innerHTML = `
                        <td><a href="singlemovie.html?id=${encodeURIComponent(movie.id)}">${movie.title}</a></td>
                        <td>${movie.year}</td>
                        <td>${movie.director}</td>
                        <td>${genreLinks}</td>
                        <td>${starLinks}</td>
                        <td>${movie.rating}</td>
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