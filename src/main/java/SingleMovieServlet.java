import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String movieId = request.getParameter("id");
        if (movieId == null || movieId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Missing movie id\"}");
            out.close();
            return;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);

            String query =
                    "WITH StarMovieCounts AS ( " +
                            "    SELECT starId, COUNT(DISTINCT movieId) AS movieCount " +
                            "    FROM stars_in_movies " +
                            "    GROUP BY starId " +
                            ") " +
                            "SELECT " +
                            "    m.id, m.title, m.year, m.director, r.rating, " +
                            "    GROUP_CONCAT(DISTINCT CONCAT(g.id, ':', g.name) ORDER BY g.name SEPARATOR ',') AS genres, " +
                            "    GROUP_CONCAT( " +
                            "        DISTINCT CONCAT(s.id, ':', s.name) " +
                            "        ORDER BY COALESCE(smc.movieCount, 0) DESC, s.name ASC " +
                            "        SEPARATOR ',' " +
                            "    ) AS stars " +
                            "FROM movies m " +
                            "JOIN ratings r ON m.id = r.movieId " +
                            "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                            "LEFT JOIN genres g ON gm.genreId = g.id " +
                            "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                            "LEFT JOIN stars s ON sm.starId = s.id " +
                            "LEFT JOIN StarMovieCounts smc ON s.id = smc.starId " +
                            "WHERE m.id = ? " +
                            "GROUP BY m.id, m.title, m.year, m.director, r.rating;";

            statement = connection.prepareStatement(query);
            statement.setString(1, movieId);
            resultSet = statement.executeQuery();

            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"movies\":[");

            boolean first = true;
            if (resultSet.next()) {
                first = false;

                String genres = escapeJson(resultSet.getString("genres"));
                String stars = escapeJson(resultSet.getString("stars"));

                jsonBuilder.append("{")
                        .append("\"id\":\"").append(escapeJson(resultSet.getString("id"))).append("\",")
                        .append("\"title\":\"").append(escapeJson(resultSet.getString("title"))).append("\",")
                        .append("\"year\":").append(resultSet.getInt("year")).append(",")
                        .append("\"director\":\"").append(escapeJson(resultSet.getString("director"))).append("\",")
                        .append("\"rating\":").append(resultSet.getDouble("rating")).append(",")
                        .append("\"genres\":\"").append(genres != null ? genres : "").append("\",")
                        .append("\"stars\":\"").append(stars != null ? stars : "").append("\"")
                        .append("}");
            }

            jsonBuilder.append("]}");
            out.write(jsonBuilder.toString());

        } catch (Exception e) {
            request.getServletContext().log("SQL Error in SingleMovieServlet for movie ID: " + movieId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Database error occurred. " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (resultSet != null) resultSet.close(); } catch (Exception e) { request.getServletContext().log("Error closing ResultSet", e); }
            try { if (statement != null) statement.close(); } catch (Exception e) { request.getServletContext().log("Error closing Statement", e); }
            try { if (connection != null) connection.close(); } catch (Exception e) { request.getServletContext().log("Error closing Connection", e); }
            if (out != null) out.close();
        }
    }

    private String escapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}