export function verify_login() {
    fetch("session-check", {
        method: "GET",
        credentials: "include"
    })
        .then(response => response.json())
        .then(data => {
            if (!data.loggedIn) {
                window.location.href = "login.html";
            }
        })
        .catch(error => {
            console.error("Session check failed:", error);
            window.location.href = "login.html";
        });
}