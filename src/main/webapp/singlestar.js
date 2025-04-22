import { verify_login } from './auth.js';
verify_login();

const urlParams = new URLSearchParams(window.location.search);
const starId = urlParams.get("id");

document.addEventListener("DOMContentLoaded", function() {
    fetch_single_star();
});

function fetch_single_star() {
    if (!starId) {
        document.getElementById("star-details").innerHTML = "<p>Error: No star ID provided</p>";
    } else {
        console.log(`Star ID: ${starId}`);

        fetch(`api/star?id=${starId}`)
            .then(response => {
                console.log("Response status:", response.status);
                return response.text();
            })
            .then(text => {
                console.log("Response text:", text)
                try {
                    const data = JSON.parse(text); // Attempt to parse it as JSON
                    const container = document.getElementById("star-details");

                    if (data.error) {
                        container.innerHTML = `<p>Error: ${data.error}</p>`;
                        return;
                    }

                    // Display star details
                    let html = `
                    <h2>${data.starInfo.starName || "Unknown Name"}</h2>
                    <p><span class="label">Birth Year:</span> ${data.starInfo.birthYear === null ? "N/A" : data.starInfo.birthYear}</p>
                    <h3>Movies:</h3>
                `;
                    if (!Array.isArray(data.starInfo.movies) || data.starInfo.movies.length === 0) {
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
                } catch (e) {
                    console.error("Error parsing JSON:", e);
                    document.getElementById("star-details").innerHTML = `<p>Error loading star: ${e.message}</p>`;
                }
            })
            .catch(error => {
                console.error("Error fetching star:", error);
                document.getElementById("star-details").innerHTML = `<p>Error loading star: ${error.message}</p>`;
            });
    }
}