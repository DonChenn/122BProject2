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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/place-order")
public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to lookup DataSource", e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("email") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "User not logged in.");
            out.write(responseJsonObject.toString());
            out.close();
            return;
        }
        String userEmail = (String) session.getAttribute("email");

        @SuppressWarnings("unchecked")
        Map<String, CartMovie> cart = (Map<String, CartMovie>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Shopping cart is empty.");
            out.write(responseJsonObject.toString());
            out.close();
            return;
        }

        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String ccNumber = request.getParameter("cc_number");
        String ccExpiry = request.getParameter("cc_expiry");

        if (firstName == null || lastName == null || ccNumber == null || ccExpiry == null ||
                firstName.trim().isEmpty() || lastName.trim().isEmpty() || ccNumber.trim().isEmpty() || ccExpiry.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Missing payment information.");
            out.write(responseJsonObject.toString());
            out.close();
            return;
        }

        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(ccExpiry.trim());
        } catch (DateTimeParseException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Invalid expiration date format. Use YYYY-MM-DD.");
            out.write(responseJsonObject.toString());
            out.close();
            return;
        }

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            String ccQuery = "SELECT id FROM creditcards WHERE id = ? AND firstName = ? AND lastName = ? AND expiration = ?";
            boolean paymentValid = false;
            try (PreparedStatement ccStatement = conn.prepareStatement(ccQuery)) {
                ccStatement.setString(1, ccNumber.trim());
                ccStatement.setString(2, firstName.trim());
                ccStatement.setString(3, lastName.trim());
                ccStatement.setDate(4, java.sql.Date.valueOf(expiryDate));

                try (ResultSet rs = ccStatement.executeQuery()) {
                    if (rs.next()) {
                        paymentValid = true;
                    }
                }
            }

            if (!paymentValid) {
                conn.rollback();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Invalid credit card information or card expired.");
                out.write(responseJsonObject.toString());
                out.close();
                return;
            }

            Integer customerId = null;
            String customerQuery = "SELECT id FROM customers WHERE email = ? LIMIT 1";
            try (PreparedStatement customerStatement = conn.prepareStatement(customerQuery)) {
                customerStatement.setString(1, userEmail);
                try (ResultSet rs = customerStatement.executeQuery()) {
                    if (rs.next()) {
                        customerId = rs.getInt("id");
                    }
                }
            }

            if (customerId == null) {
                conn.rollback();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Could not find customer record for logged-in user.");
                out.write(responseJsonObject.toString());
                out.close();
                return;
            }

            String salesInsertQuery = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?)";
            try (PreparedStatement salesStatement = conn.prepareStatement(salesInsertQuery)) {
                LocalDate saleDate = LocalDate.now();

                for (Map.Entry<String, CartMovie> entry : cart.entrySet()) {
                    CartMovie item = entry.getValue();
                    int quantity = item.getQuantity();
                    for (int i = 0; i < quantity; i++) {
                        salesStatement.setInt(1, customerId);
                        salesStatement.setString(2, item.getMovieId());
                        salesStatement.setDate(3, java.sql.Date.valueOf(saleDate));
                        salesStatement.addBatch();
                    }
                }

                int[] updateCounts = salesStatement.executeBatch();

                for (int count : updateCounts) {
                    if (count == PreparedStatement.EXECUTE_FAILED) {
                        throw new SQLException("Batch insert failed for at least one statement.");
                    }
                }

            }

            conn.commit();

            session.removeAttribute("cart");
            response.setStatus(HttpServletResponse.SC_OK);
            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("message", "Order placed successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Database error occurred during order placement: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "An unexpected error occurred: " + e.getMessage());
        }
        finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            out.write(responseJsonObject.toString());
            out.close();
        }
    }
}
