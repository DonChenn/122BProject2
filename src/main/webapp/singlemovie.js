import { verify_login } from './auth.js';
verify_login();

const urlParams = new URLSearchParams(window.location.search);
const movieId = urlParams.get("id");

document.addEventListener("DOMContentLoaded", function() {
    fetch_single_movie();
});

function fetch_single_movie() {
    fetch(`api/movie?id=${movieId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            const container = document.getElementById("movie-details");

            if (data.error) {
                container.innerHTML = `<p>${data.error}</p>`;
                return;
            }

            if (!data.movies || data.movies.length === 0) {
                container.innerHTML = `<p>No movie found with ID: ${movieId}</p>`;
                return;
            }

            const movie = data.movies[0];

            let genresHTML = 'N/A';
            if (movie.genres) {
                genresHTML = movie.genres.split(',').map(genre => {
                    const [id, name] = genre.split(':');
                    if (id && name) {
                        const trimmedId = id.trim();
                        const trimmedName = name.trim();
                        const genreUrl = `movies.html?genreId=${encodeURIComponent(trimmedId)}&genre=${encodeURIComponent(trimmedName)}&page=1`;
                        return `<a href="${genreUrl}">${name.trim()}</a>`;
                    }
                    return genre.trim();
                }).join(', ');
            }

            let starsHTML = 'N/A';
            if (movie.stars) {
                starsHTML = movie.stars.split(',').map(star => {
                    const [id, name] = star.split(':');
                    if (id && name) {
                        return `<a href="singlestar.html?id=${id.trim()}">${name.trim()}</a>`;
                    }
                    return star.trim();
                }).join(', ');
            }

            container.innerHTML = `
                <h2>${movie.title} (${movie.year})</h2>
                <p><span class="label">Director:</span> ${movie.director}</p>
                <p><span class="label">Rating:</span> ${movie.rating || 'N/A'}</p>
                <p><span class="label">Genres:</span> ${genresHTML}</p>
                <p><span class="label">Stars:</span> ${starsHTML}</p>
            `;
        })
        .catch(error => {
            console.error("Error fetching movie:", error);
            document.getElementById("movie-details").innerHTML = `<p>Error loading movie: ${error.message}</p>`;
        });
}