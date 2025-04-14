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

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "mytestuser";
        String loginPasswd = "password123";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String starId = request.getParameter("id");
        System.out.println("Received star ID: " + starId);

        if (starId == null || starId.isEmpty()) {
            out.write("{\"error\": \"Missing star id\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
            System.out.println("Database connection established");

            // First, check if the star exists
            String checkStarQuery = "SELECT id, name, birthYear FROM stars WHERE id = ?";
            PreparedStatement checkStarStatement = connection.prepareStatement(checkStarQuery);
            checkStarStatement.setString(1, starId);
            ResultSet checkStarResult = checkStarStatement.executeQuery();

            if (!checkStarResult.next()) {
                System.out.println("Star not found in database: " + starId);
                out.write("{\"error\": \"Star not found\"}");
                checkStarResult.close();
                checkStarStatement.close();
                connection.close();
                return;
            }

            // Store basic star info
            String starIdVal = escapeJson(checkStarResult.getString("id"));
            String starNameVal = escapeJson(checkStarResult.getString("name"));
            String birthYearVal = "null";

            if (checkStarResult.getObject("birthYear") != null) {
                birthYearVal = String.valueOf(checkStarResult.getInt("birthYear"));
            }

            checkStarResult.close();
            checkStarStatement.close();

            // Get the movies the star was in with LEFT JOIN to ensure we get the star even if they have no movies
            String query =
                    "SELECT m.id AS movieId, m.title AS movieTitle, m.year, m.director " +
                            "FROM stars_in_movies sm " +
                            "JOIN movies m ON sm.movieId = m.id " +
                            "WHERE sm.starId = ?";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, starId);
            ResultSet resultSet = statement.executeQuery();

            StringBuilder movieArrayBuilder = new StringBuilder();
            movieArrayBuilder.append("[");

            boolean firstMovie = true;
            int movieCount = 0;

            while (resultSet.next()) {
                movieCount++;
                if (!firstMovie) {
                    movieArrayBuilder.append(",");
                }
                firstMovie = false;

                movieArrayBuilder.append("{")
                        .append("\"movieId\":\"").append(escapeJson(resultSet.getString("movieId"))).append("\",")
                        .append("\"title\":\"").append(escapeJson(resultSet.getString("movieTitle"))).append("\",")
                        .append("\"year\":").append(resultSet.getInt("year")).append(",")
                        .append("\"director\":\"").append(escapeJson(resultSet.getString("director"))).append("\"")
                        .append("}");
            }

            movieArrayBuilder.append("]");

            // Build the final JSON response
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{")
                    .append("\"starInfo\":{")
                    .append("\"starId\":\"").append(starIdVal).append("\",")
                    .append("\"starName\":\"").append(starNameVal).append("\",")
                    .append("\"birthYear\":").append(birthYearVal).append(",")
                    .append("\"movies\":").append(movieArrayBuilder.toString())
                    .append("}")
                    .append("}");

            out.write(jsonBuilder.toString());

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error in SingleStarServlet: " + e.getMessage());
            e.printStackTrace();
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
