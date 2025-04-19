import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;


@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Add this line

            String loginUser = "mytestuser";
            String loginPasswd = "My6$Password";
            String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC";

            Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);

            String query = "SELECT * FROM customers WHERE email = ? AND password = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, email);
            statement.setString(2, password);

            ResultSet resultSet = statement.executeQuery();

            resultSet.close();
            statement.close();
            connection.close();
            response.sendRedirect(request.getContextPath() + "/movies.html");
            return;
        } catch (Exception e) {
            request.getServletContext().log("Login error: ", e);
            out.println("<html><head><title>Error</title></head><body>");
            out.println("<p>Error during login: " + e.getMessage() + "</p>");
            out.println("</body></html>");
        }

        out.close();
    }
}
