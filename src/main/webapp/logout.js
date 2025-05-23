document.getElementById("logout-link").addEventListener("click", function(event) {
    event.preventDefault();
    console.log("Logout clicked");

    fetch("/fabflix/logout", {
        method: "GET",
        credentials: "same-origin"
    })
        .then(response => {
            console.log("Response status:", response.status);
            return response.json();
        })
        .then(data => {
            console.log("Response data:", data);
            if (data.status === "success") {
                document.location.replace("/fabflix/login.html");
            } else {
                alert("Logout failed");
            }
        })
        .catch(error => {
            console.error("Error:", error);
        });

    return false;
});