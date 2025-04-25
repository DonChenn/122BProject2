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

        String genreFilter = request.getParameter("genre");

        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
             Statement statement = connection.createStatement()) {

            String starMovieCountsCTE = "WITH StarMovieCounts AS ( " +
                    "    SELECT starId, COUNT(DISTINCT movieId) AS movieCount " +
                    "    FROM stars_in_movies " +
                    "    GROUP BY starId " +
                    "), ";
            String rankedStarsCTE = "RankedStars AS ( " +
                    "    SELECT " +
                    "        sm.movieId, " +
                    "        s.id AS starId, " +
                    "        s.name AS starName, " +
                    "        ROW_NUMBER() OVER(PARTITION BY sm.movieId ORDER BY COALESCE(smc.movieCount, 0) DESC, s.name ASC) as rn " +
                    "    FROM stars_in_movies sm " +
                    "    JOIN stars s ON sm.starId = s.id " +
                    "    LEFT JOIN StarMovieCounts smc ON s.id = smc.starId " +
                    "), ";
            String topStarsPerMovieCTE = "TopStarsPerMovie AS ( " +
                    "    SELECT " +
                    "        movieId, " +
                    "        GROUP_CONCAT(CONCAT(starId, ':', starName) ORDER BY rn SEPARATOR ',') AS topStars " +
                    "    FROM RankedStars " +
                    "    WHERE rn <= 3 " +
                    "    GROUP BY movieId " +
                    ") ";

            String mainQuerySelect = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "       GROUP_CONCAT(DISTINCT g_main.name ORDER BY g_main.name SEPARATOR ',') AS genres, " +
                    "       tspm.topStars AS stars " +
                    "FROM movies m " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gm_main ON m.id = gm_main.movieId " +
                    "LEFT JOIN genres g_main ON gm_main.genreId = g_main.id " +
                    "LEFT JOIN TopStarsPerMovie tspm ON m.id = tspm.movieId ";

            String whereClause = "";
            if (genreFilter != null && !genreFilter.trim().isEmpty()) {
                mainQuerySelect += " JOIN genres_in_movies gm_filter ON m.id = gm_filter.movieId " +
                        " JOIN genres g_filter ON gm_filter.genreId = g_filter.id ";
                whereClause = "WHERE g_filter.name = '" + escapeSQL(genreFilter) + "' ";
            }

            String groupByClause = "GROUP BY m.id, m.title, m.year, m.director, r.rating, tspm.topStars ";
            String orderByClause = "ORDER BY r.rating DESC ";
            String limitClause = "LIMIT 20;";

            String finalQuery = starMovieCountsCTE + rankedStarsCTE + topStarsPerMovieCTE +
                    mainQuerySelect +
                    whereClause +
                    groupByClause +
                    orderByClause +
                    limitClause;

            System.out.println("Executing Query: " + finalQuery);

            ResultSet resultSet = statement.executeQuery(finalQuery);

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
            request.getServletContext().log("Error fetching movies: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"An internal error occurred: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeSQL(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }
}