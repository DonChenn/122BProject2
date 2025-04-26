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
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final List<String> ALLOWED_SORT_FIELDS = Arrays.asList("title", "rating");
    private static final List<String> ALLOWED_ORDERS = Arrays.asList("asc", "desc");
    private static final List<Integer> ALLOWED_LIMITS = Arrays.asList(10, 25, 50, 100);
    private static final int DEFAULT_LIMIT = 25;
    private static final int DEFAULT_PAGE = 1;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("email") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"User not logged in.\"}");
            return;
        }

        String genreFilter = request.getParameter("genre");
        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String starName = request.getParameter("star_name");
        String titleInitial = request.getParameter("titleInitial");


        String sort1 = request.getParameter("sort1");
        String order1 = request.getParameter("order1");
        String sort2 = request.getParameter("sort2");
        String order2 = request.getParameter("order2");

        int limit = DEFAULT_LIMIT;
        int page = DEFAULT_PAGE;

        try {
            int requestedLimit = Integer.parseInt(request.getParameter("limit"));
            if (ALLOWED_LIMITS.contains(requestedLimit)) {
                limit = requestedLimit;
            }
        } catch (NumberFormatException | NullPointerException e) {
            // Use default
        }

        try {
            page = Integer.parseInt(request.getParameter("page"));
            if (page < 1) {
                page = DEFAULT_PAGE;
            }
        } catch (NumberFormatException | NullPointerException e) {
            // Use default
        }

        int offset = (page - 1) * limit;

        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        JsonObject jsonResponse = new JsonObject();

        String queryString = request.getQueryString();
        String currentUrl = "movies.html" + (queryString != null ? "?" + queryString : "");

        if (session != null) {
            session.setAttribute("movieListUrl", currentUrl);
            System.out.println("Stored movieListUrl in session: " + currentUrl);
        }

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
                    "        GROUP_CONCAT(CONCAT(starId, ':', starName) ORDER BY rn SEPARATOR ', ') AS topStars " +
                    "    FROM RankedStars " +
                    "    WHERE rn <= 3 " +
                    "    GROUP BY movieId " +
                    ") ";

            String mainQuerySelect = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "       GROUP_CONCAT(DISTINCT g_main.name ORDER BY g_main.name SEPARATOR ', ') AS genres, " +
                    "       tspm.topStars AS stars " +
                    "FROM movies m " +
                    "JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gm_main ON m.id = gm_main.movieId " +
                    "LEFT JOIN genres g_main ON gm_main.genreId = g_main.id " +
                    "LEFT JOIN TopStarsPerMovie tspm ON m.id = tspm.movieId ";

            List<String> conditions = new ArrayList<>();
            String whereClause = "";

            if (genreFilter != null && !genreFilter.trim().isEmpty()) {
                mainQuerySelect += " JOIN genres_in_movies gm_filter ON m.id = gm_filter.movieId " +
                        " JOIN genres g_filter ON gm_filter.genreId = g_filter.id ";
                conditions.add("g_filter.name = '" + escapeSQL(genreFilter.trim()) + "'");
            }
            if (title != null && !title.trim().isEmpty()) {
                conditions.add("m.title LIKE '%" + escapeSQL(title.trim()) + "%'");
            }
            if (year != null && !year.trim().isEmpty()) {
                if (year.trim().matches("\\d{4}")) {
                    conditions.add("m.year = " + year.trim());
                }
            }
            if (director != null && !director.trim().isEmpty()) {
                conditions.add("m.director LIKE '%" + escapeSQL(director.trim()) + "%'");
            }
            if (starName != null && !starName.trim().isEmpty()) {
                conditions.add("EXISTS (SELECT 1 FROM stars_in_movies sim_check " +
                        "JOIN stars s_check ON sim_check.starId = s_check.id " +
                        "WHERE sim_check.movieId = m.id AND s_check.name LIKE '%" + escapeSQL(starName.trim()) + "%')");
            }

            if (titleInitial != null && !titleInitial.trim().isEmpty()) {
                if (titleInitial.equals("*")) {
                    conditions.add("m.title REGEXP '^[^a-zA-Z0-9]'");
                } else {
                    conditions.add("m.title LIKE '" + escapeSQL(titleInitial.trim()) + "%'");
                }
            }

            if (!conditions.isEmpty()) {
                whereClause = "WHERE " + String.join(" AND ", conditions) + " ";
            }

            String groupByClause = "GROUP BY m.id, m.title, m.year, m.director, r.rating, tspm.topStars ";

            StringBuilder orderByBuilder = new StringBuilder("ORDER BY ");
            boolean firstSortParam = true;
            if (isValidSortParam(sort1, order1)) {
                orderByBuilder.append(getColumnForSortField(sort1)).append(" ").append(order1.toLowerCase());
                firstSortParam = false;
            }
            if (isValidSortParam(sort2, order2)) {
                if (!firstSortParam) orderByBuilder.append(", ");
                if (!getColumnForSortField(sort1).equalsIgnoreCase(getColumnForSortField(sort2))) {
                    orderByBuilder.append(getColumnForSortField(sort2)).append(" ").append(order2.toLowerCase());
                    firstSortParam = false;
                } else if (firstSortParam) {
                    orderByBuilder.append(getColumnForSortField(sort2)).append(" ").append(order2.toLowerCase());
                    firstSortParam = false;
                }
            }
            if (firstSortParam) {
                orderByBuilder.append("r.rating DESC, m.title ASC");
            }
            String orderByClause = orderByBuilder.toString() + " ";

            String limitOffsetClause = "LIMIT " + limit + " OFFSET " + offset;

            String finalQuery = starMovieCountsCTE + rankedStarsCTE + topStarsPerMovieCTE +
                    mainQuerySelect +
                    whereClause +
                    groupByClause +
                    orderByClause +
                    limitOffsetClause + ";";

            System.out.println("Executing Query (" + limit + " results, page " + page + "): " + finalQuery);

            ResultSet resultSet = statement.executeQuery(finalQuery);
            JsonArray moviesArray = new JsonArray();
            int resultsCount = 0;
            while (resultSet.next()) {
                resultsCount++;
                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("id", resultSet.getString("id"));
                movieJson.addProperty("title", resultSet.getString("title"));
                movieJson.addProperty("year", resultSet.getInt("year"));
                movieJson.addProperty("director", resultSet.getString("director"));
                double rating = resultSet.getDouble("rating");
                if (!resultSet.wasNull()) {
                    movieJson.addProperty("rating", rating);
                } else {
                    movieJson.addProperty("rating", "N/A");
                }
                movieJson.addProperty("genres", resultSet.getString("genres"));
                movieJson.addProperty("stars", resultSet.getString("stars"));

                moviesArray.add(movieJson);
            }
            resultSet.close();

            jsonResponse.add("movies", moviesArray);
            jsonResponse.addProperty("currentPage", page);
            jsonResponse.addProperty("limit", limit);
            jsonResponse.addProperty("hasMoreResults", resultsCount == limit);

            out.write(gson.toJson(jsonResponse));

        } catch (Exception e) {
            request.getServletContext().log("Error fetching movies: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "An internal error occurred while fetching movies.");
            errorResponse.addProperty("detail", e.getMessage());
            out.write(gson.toJson(errorResponse));
        }
    }

    private boolean isValidSortParam(String field, String order) {
        return field != null && !field.trim().isEmpty() &&
                order != null && !order.trim().isEmpty() &&
                ALLOWED_SORT_FIELDS.contains(field.toLowerCase()) &&
                ALLOWED_ORDERS.contains(order.toLowerCase());
    }

    private String getColumnForSortField(String field) {
        if (field == null) return "r.rating";
        String lowerField = field.toLowerCase();
        if ("title".equals(lowerField)) {
            return "m.title";
        } else if ("rating".equals(lowerField)) {
            return "r.rating";
        }
        return "r.rating";
    }

    private String escapeSQL(String s) {
        if (s == null) return "";
        return s.replace("'", "''").replace("\\", "\\\\");
    }
}