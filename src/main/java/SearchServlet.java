import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet(name = "SearchServlet", urlPatterns = "/search")
public class SearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String starName = request.getParameter("star_name");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String loginUser = "mytestuser";
            String loginPasswd = "My6$Password";
            String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

            try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {

                StringBuilder query = new StringBuilder(
                        "SELECT DISTINCT movies.id, movies.title, movies.year, movies.director " +
                                "FROM movies " +
                                "LEFT JOIN stars_in_movies ON movies.id = stars_in_movies.movieId " +
                                "LEFT JOIN stars ON stars_in_movies.starId = stars.id " +
                                "WHERE 1=1 "
                );

                if (title != null && !title.trim().isEmpty()) {
                    query.append("AND movies.title LIKE ? ");
                }
                if (year != null && !year.trim().isEmpty()) {
                    query.append("AND movies.year = ? ");
                }
                if (director != null && !director.trim().isEmpty()) {
                    query.append("AND movies.director LIKE ? ");
                }
                if (starName != null && !starName.trim().isEmpty()) {
                    query.append("AND stars.name LIKE ? ");
                }

                PreparedStatement statement = connection.prepareStatement(query.toString());

                int paramIndex = 1;
                if (title != null && !title.trim().isEmpty()) {
                    statement.setString(paramIndex++, "%" + title.trim() + "%");
                }
                if (year != null && !year.trim().isEmpty()) {
                    statement.setInt(paramIndex++, Integer.parseInt(year.trim()));
                }
                if (director != null && !director.trim().isEmpty()) {
                    statement.setString(paramIndex++, "%" + director.trim() + "%");
                }
                if (starName != null && !starName.trim().isEmpty()) {
                    statement.setString(paramIndex++, "%" + starName.trim() + "%");
                }

                ResultSet resultSet = statement.executeQuery();

                JSONArray jsonArray = new JSONArray();

                while (resultSet.next()) {
                    JSONObject movie = new JSONObject();
                    movie.put("id", resultSet.getString("id"));
                    movie.put("title", resultSet.getString("title"));
                    movie.put("year", resultSet.getInt("year"));
                    movie.put("director", resultSet.getString("director"));
                    jsonArray.put(movie);
                }

                out.write(jsonArray.toString());
            }

        } catch (Exception e) {
            request.getServletContext().log("Search error: ", e);
            JSONObject error = new JSONObject();
            error.put("status", "fail");
            error.put("message", "Internal error: " + e.getMessage());
            out.write(error.toString());
        }

        out.close();
    }
}
