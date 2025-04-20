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

        System.out.println("DEBUG: Raw Email = " + email);
        System.out.println("DEBUG: Raw Password = " + password);

        if (email == null || password == null) {
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"fail\", \"message\":\"Missing email or password\"}");
            return;
        }

        email = email.trim();
        password = password.trim();

        System.out.println("DEBUG: Email = '" + email + "'");
        System.out.println("DEBUG: Password = '" + password + "'");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("DEBUG: JDBC Driver loaded");

            String loginUser = "mytestuser";
            String loginPasswd = "My6$Password";
            String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC";

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
                System.out.println("DEBUG: DB Connection successful");

                String query = "SELECT * FROM customers WHERE email = ? AND password = ?";
                PreparedStatement statement = connection.prepareStatement(query);

                statement.setString(1, email);
                statement.setString(2, password);
                System.out.println("DEBUG: Executing query -> " + query);

                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    System.out.println("DEBUG: Login successful for " + email);
                    HttpSession session = request.getSession();
                    session.setAttribute("email", email);

                    out.write("{\"status\":\"success\"}");
                } else {
                    System.out.println("DEBUG: Login failed - no matching record found");
                    out.write("{\"status\":\"fail\", \"message\":\"Invalid email or password.\"}");
                }
            }

        } catch (Exception e) {
            request.getServletContext().log("Login error: ", e);
            System.out.println("DEBUG: Exception caught: " + e.getMessage());
            out.write("{\"status\":\"fail\", \"message\":\"Internal error: " + e.getMessage() + "\"}");
        }

        out.close();
    }
}
