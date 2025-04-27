import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AddToCartServlet", urlPatterns = "/api/add-to-cart")
public class AddToCartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        String movieId = request.getParameter("movieId");

        if (movieId == null || movieId.trim().isEmpty()) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Movie ID is required.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(responseJsonObject.toString());
            out.close();
            return;
        }

        HttpSession session = request.getSession();
        Map<String, CartMovie> cart = (Map<String, CartMovie>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        try (Connection conn = dataSource.getConnection()) {
            if (cart.containsKey(movieId)) {
                cart.get(movieId).incrementQuantity();
                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "Increased quantity for item: " + movieId);
                responseJsonObject.addProperty("itemId", movieId);

            } else {
                String query = "SELECT title FROM movies WHERE id = ? LIMIT 1";
                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    statement.setString(1, movieId);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            String movieTitle = rs.getString("title");
                            BigDecimal price = new BigDecimal("5.00");

                            CartMovie newItem = new CartMovie(movieId, movieTitle, price);
                            cart.put(movieId, newItem);

                            responseJsonObject.addProperty("status", "success");
                            responseJsonObject.addProperty("message", "Item added to cart: " + movieTitle);
                            responseJsonObject.addProperty("itemId", movieId);
                            responseJsonObject.addProperty("itemTitle", movieTitle);
                        } else {
                            responseJsonObject.addProperty("status", "fail");
                            responseJsonObject.addProperty("message", "Movie not found with ID: " + movieId);
                            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        }
                    }
                }
            }
            session.setAttribute("cart", cart);
            response.setStatus(HttpServletResponse.SC_OK);


        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Error adding item to cart: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            out.write(responseJsonObject.toString());
            out.close();
        }
    }
}