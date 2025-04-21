import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("email") == null) {
            response.sendRedirect("login.html");
            return;
        }

        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
             Statement statement = connection.createStatement()) {

            // Modified query to get movies with ratings, genres, and stars (including star IDs)
            String query = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ',') AS genres, " +
                    "GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars " +
                    "FROM movies m " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                    "LEFT JOIN genres g ON gm.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                    "LEFT JOIN stars s ON sm.starId = s.id " +
                    "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                    "ORDER BY r.rating DESC " +
                    "LIMIT 20;";

            ResultSet resultSet = statement.executeQuery(query);

            // Create JSON array for movies
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"movies\":[");

            boolean first = true;
            while (resultSet.next()) {
                if (!first) {
                    jsonBuilder.append(", ");
                }
                first = false;

                jsonBuilder.append("{")
                        .append("\"id\":\"").append(escapeJson(resultSet.getString("id"))).append("\",")
                        .append("\"title\":\"").append(escapeJson(resultSet.getString("title"))).append("\",")
                        .append("\"year\":").append(resultSet.getInt("year")).append(",")
                        .append("\"director\":\"").append(escapeJson(resultSet.getString("director"))).append("\",")
                        .append("\"rating\":").append(resultSet.getDouble("rating")).append(",")
                        .append("\"genres\":\"").append(escapeJson(resultSet.getString("genres"))).append("\",")
                        .append("\"stars\":\"").append(escapeJson(resultSet.getString("stars"))).append("\"")
                        .append("}");
            }

            jsonBuilder.append("]}");
            out.write(jsonBuilder.toString());

        } catch (Exception e) {
            request.getServletContext().log("Error: ", e);
            out.write("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
        out.close();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
