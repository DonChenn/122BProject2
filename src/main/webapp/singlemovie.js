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

        // Parse stars data in format "id:name,id:name"
        const starsHTML = movie.stars.split(',').map(star => {
            const [id, name] = star.split(':');
            if (id && name) {
                return `<a href="singlestar.html?id=${id.trim()}">${name.trim()}</a>`;
            }
            return star.trim();
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