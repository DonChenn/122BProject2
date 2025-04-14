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

// This annotation maps this Java Servlet Class to a URL
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        // Set response to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();


        // GETS THE ID
        String movieId = request.getParameter("id");
        if (movieId == null || movieId.isEmpty()) {
            out.write("{\"error\": \"Missing movie id\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);

            // Modified query to get movies with ratings, genres, and stars
            String query = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ',') AS genres, " +
                    "GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) ORDER BY s.name SEPARATOR ',') AS stars " +
                    "FROM movies m " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                    "LEFT JOIN genres g ON gm.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                    "LEFT JOIN stars s ON sm.starId = s.id " +
                    "WHERE m.id = ? " +
                    "GROUP BY m.id, m.title, m.year, m.director, r.rating;";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, movieId);
            ResultSet resultSet = statement.executeQuery();

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

            resultSet.close();
            statement.close();
            connection.close();

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
