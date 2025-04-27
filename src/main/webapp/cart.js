import { verify_login } from './auth.js';
verify_login();

function handleCartData(resultDataJson) {
    console.log("handleCartData: received data", resultDataJson);

    let cartTableBodyElement = jQuery("#cart_table_body");
    cartTableBodyElement.empty();

    let totalPrice = 0;
    const cartItems = resultDataJson.cart_items || [];

    if (cartItems.length === 0) {
        cartTableBodyElement.append("<tr><td colspan='5'>Your cart is empty.</td></tr>");
        jQuery("#total_price").text("0.00");
        return;
    }

    for (let i = 0; i < cartItems.length; i++) {
        let item = cartItems[i];
        let subtotal = item.quantity * item.price;
        totalPrice += subtotal;

        let rowHTML = "<tr>";
        rowHTML += "<td>" + item.movie_title + "</td>";
        rowHTML += "<td class='quantity-controls'>" +
            "<button class='btn btn-secondary btn-sm decrease-qty' data-movie-id='" + item.movie_id + "'>-</button> " +
            "<span class='item-qty'>" + item.quantity + "</span> " +
            "<button class='btn btn-secondary btn-sm increase-qty' data-movie-id='" + item.movie_id + "'>+</button>" +
            "</td>";
        rowHTML += "<td>$" + item.price.toFixed(2) + "</td>";
        rowHTML += "<td>$" + subtotal.toFixed(2) + "</td>";
        rowHTML += "<td>" +
            "<button class='btn btn-danger btn-sm remove-item' data-movie-id='" + item.movie_id + "'>Remove</button>" +
            "</td>";
        rowHTML += "</tr>";
        cartTableBodyElement.append(rowHTML);
    } 

    jQuery("#total_price").text(totalPrice.toFixed(2));

    $(".increase-qty").click(function() {
        let movieId = $(this).data("movie-id");
        updateQuantity(movieId, 'increase');
    });

    $(".decrease-qty").click(function() {
        let movieId = $(this).data("movie-id");
        let quantitySpan = $(this).closest('td').find('.item-qty');

        let currentQuantity = parseInt(quantitySpan.text());

        if (currentQuantity === 1) {
            if (confirm("Are you sure you want to remove this item?")) {
                updateQuantity(movieId, 'remove');
            }
        } else if (currentQuantity > 1) {
            updateQuantity(movieId, 'decrease');
        }
    });

    $(".remove-item").click(function() {
        let movieId = $(this).data("movie-id");
        if (confirm("Are you sure you want to remove this item?")) {
            updateQuantity(movieId, 'remove');
        }
    });
}

function updateQuantity(movieId, action) {
    console.log("updateQuantity: movieId=" + movieId + ", action=" + action);
    jQuery.ajax({
        dataType: "json",
        method: "POST",
        url: "api/shopping-cart",
        data: {
            'movie_id': movieId,
            'action': action
        },
        success: (response) => {
            console.log("Update successful", response);
            fetchCartData();
        },
        error: (jqXHR, textStatus, errorThrown) => {
            console.error("Update failed: " + textStatus, errorThrown);
            alert("Failed to update cart. Please try again.");
        }
    });
}

function fetchCartData() {
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/shopping-cart",
        success: (resultData) => handleCartData(resultData)
    });
}

jQuery(document).ready(function() {
    fetchCartData();
});