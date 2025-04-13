fetch("api/movies")
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        console.log("Data received:", data);
        const tableBody = document.querySelector("#movies-table tbody");

        if (!data.movies || data.movies.length === 0) {
            console.log("No movies found in the response");
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 6;
            cell.textContent = "No movies found";
            row.appendChild(cell);
            tableBody.appendChild(row);
            return;
        }

        data.movies.forEach(movie => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td><a href="#">${movie.title}</a></td>
                <td>${movie.year}</td>
                <td>${movie.director}</td>
                <td>${(movie.genres || "").replace(/,/g, ", ")}</td>
                <td>${
                    (movie.stars || "")
                    .split(",")
                    .map(star => `<a href="#">${star.trim()}</a>`)
                    .join(", ")
                }</td>
                <td>${movie.rating}</td>
            `;
            tableBody.appendChild(row);
        });
    })
    .catch(error => {
        console.error("Error fetching movies:", error);
        const tableBody = document.querySelector("#movies-table tbody");
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 6;
        cell.textContent = "Error loading movies: " + error.message;
        row.appendChild(cell);
        tableBody.appendChild(row);
    });