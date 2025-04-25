import { verify_login } from './auth.js';
verify_login();

document.addEventListener("DOMContentLoaded", function() {
    fetch_movies();
});

function fetch_movies() {
    const moviesDetailsDiv = document.getElementById("movies-details");

    const urlParams = new URLSearchParams(window.location.search);
    const genreFilter = urlParams.get('genre');

    let apiUrl = "api/movies";
    if (genreFilter) {
        apiUrl += "?genre=" + encodeURIComponent(genreFilter);
        console.log("Fetching movies filtered by genre:", genreFilter);
    }

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