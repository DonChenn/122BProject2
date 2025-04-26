import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "BrowseGenreServlet", urlPatterns = "/api/genres")
public class BrowseGenreServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
             Statement statement = connection.createStatement()) {

            String query = "SELECT name FROM genres ORDER BY name ASC";
            ResultSet resultSet = statement.executeQuery(query);

            List<String> genres = new ArrayList<>();
            while (resultSet.next()) {
                genres.add(resultSet.getString("name"));
            }

            out.write("{\"genres\":[");
            for (int i = 0; i < genres.size(); i++) {
                if (i > 0) out.write(",");
                out.write("\"" + escapeJson(genres.get(i)) + "\"");
            }
            out.write("]}");

        } catch (Exception e) {
            request.getServletContext().log("Error fetching genres: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"An internal error occurred.\"}");
        } finally {
            out.close();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
