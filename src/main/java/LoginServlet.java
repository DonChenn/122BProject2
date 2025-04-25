import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null) {
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"fail\", \"message\":\"Missing email or password\"}");
            return;
        }

        email = email.trim();
        password = password.trim();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String loginUser = "mytestuser";
            String loginPasswd = "My6$Password";
            String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {

                String query = "SELECT password FROM customers WHERE email = ?";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, email);
                ResultSet resultSet = statement.executeQuery();

                if (!resultSet.next()) {
                    out.write("{\"status\":\"fail\", \"message\":\"Email not found.\"}");
                } else {
                    String storedPassword = resultSet.getString("password");

                    if (!storedPassword.equals(password)) {
                        out.write("{\"status\":\"fail\", \"message\":\"Incorrect password.\"}");
                    } else {
                        HttpSession session = request.getSession();
                        session.setAttribute("email", email);
                        out.write("{\"status\":\"success\"}");
                    }
                }
            }

        } catch (Exception e) {
            request.getServletContext().log("Login error: ", e);
            out.write("{\"status\":\"fail\", \"message\":\"Internal error: " + e.getMessage() + "\"}");
        }

        out.close();
    }

}
