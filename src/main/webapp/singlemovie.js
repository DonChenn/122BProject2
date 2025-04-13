const urlParams = new URLSearchParams(window.location.search);
const movieId = urlParams.get("id");

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

        // Turn stars into links if needed (assumes stars is a comma-separated string)
        const starsHTML = movie.stars.split(',').map(star => {
            return `<a href="single-star.html?name=${encodeURIComponent(star.trim())}">${star.trim()}</a>`;
        }).join(', ');

        container.innerHTML = `
                    <h2>${movie.title} (${movie.year})</h2>
                    <p><span class="label">Director:</span> ${movie.director}</p>
                    <p><span class="label">Rating:</span> ${movie.rating}</p>
                    <p><span class="label">Genres:</span> ${movie.genres}</p>
                    <p><span class="label">Stars:</span> ${starsHTML}</p>
                `;
    })
    .catch(error => {
        console.error("Error fetching movie:", error);
        document.getElementById("movie-details").innerHTML = `<p>Error loading movie: ${error.message}</p>`;
    });