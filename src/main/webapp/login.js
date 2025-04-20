const form = document.getElementById("login-form");
const messageBox = document.getElementById("message");

form.addEventListener("submit", function (event) {
    event.preventDefault();

    // Manually extract and encode form data
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const formData = new URLSearchParams();
    formData.append("email", email);
    formData.append("password", password);

    fetch("login", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formData.toString()
    })
        .then(response => response.json())
        .then(data => {
            if (data.status === "success") {
                window.location.href = "movies.html";
            } else {
                messageBox.textContent = data.message;
            }
        })
        .catch(error => {
            console.error("Login error:", error);
            messageBox.textContent = "Something went wrong. Try again.";
        });
});