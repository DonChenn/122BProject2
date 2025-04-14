// Get the `id` parameter from the URL
const urlParams = new URLSearchParams(window.location.search);
const starId = urlParams.get("id");

if (!starId) {
    document.getElementById("star-details").innerHTML = "<p>Error: No star ID provided</p>";
} else {
    // Log the starId to check if it's being passed correctly
    console.log(`Star ID: ${starId}`);

    // Fetch star details with the provided star ID
    fetch(`api/star?id=${starId}`)
        .then(response => response.json())
        .then(data => {
            const container = document.getElementById("star-details");

            if (data.error) {
                container.innerHTML = `<p>Error: ${data.error}</p>`;
                return;
            }

            // Display star details (this part should match your HTML structure)
            let html = `
                <h2>${data.starInfo.starName || "Unknown Name"}</h2>
                <p><span class="label">Birth Year:</span> ${data.starInfo.birthYear === null ? "N/A" : data.starInfo.birthYear}</p>
                <h3>Movies:</h3>
            `;
            if (!data.starInfo.movies || data.starInfo.movies.length === 0) {
                html += "<p>This star hasn't appeared in any movies.</p>";
            } else {
                html += "<ul>";
                data.starInfo.movies.forEach(movie => {
                    html += `<li>
                        <a href="singlemovie.html?id=${movie.movieId}">
                            ${movie.title} (${movie.year}) - Directed by ${movie.director}
                        </a>
                    </li>`;
                });
                html += "</ul>";
            }

            container.innerHTML = html;
        })
        .catch(error => {
            console.error("Error fetching star:", error);
            document.getElementById("star-details").innerHTML = `<p>Error loading star: ${error.message}</p>`;
        });
}
