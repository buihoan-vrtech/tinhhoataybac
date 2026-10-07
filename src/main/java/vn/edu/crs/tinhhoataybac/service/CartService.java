package vn.edu.crs.tinhhoataybac.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.model.CartItem;
import vn.edu.crs.tinhhoataybac.model.Product;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private static final String CART_SESSION_KEY = "cart";


    @SuppressWarnings("unchecked")
    public List<CartItem> getCart(HttpSession session) {

        Object cartObject = session.getAttribute(CART_SESSION_KEY);

        if (cartObject == null) {

            List<CartItem> newCart = new ArrayList<>();

            session.setAttribute(CART_SESSION_KEY, newCart);

            return newCart;
        }

        return (List<CartItem>) cartObject;
    }


    public void addToCart(HttpSession session,
                          Product product,
                          double quantity) {

        product.validateQuantity(quantity);
        List<CartItem> cart = getCart(session);
        if(product.getStock()==null||product.getStock()<=0||quantity<=0)throw new IllegalStateException("Sản phẩm đã hết hàng.");
        for (CartItem item : cart) {

            if (item.getProduct().getId().equals(product.getId())) {

                item.setQuantity(
                        Math.min(product.getStock(), Math.round((item.getQuantity() + quantity)*100.0)/100.0)
                );

                session.setAttribute(CART_SESSION_KEY, cart);

                return;
            }
        }

        CartItem newItem = new CartItem(
                product,
                quantity
        );

        cart.add(newItem);

        session.setAttribute(
                CART_SESSION_KEY,
                cart
        );
    }


    public void updateQuantity(HttpSession session,
                               Long productId,
                               double quantity) {

        List<CartItem> cart = getCart(session);

        for (CartItem item : cart) {

            if (item.getProduct().getId().equals(productId)) {

                if (quantity <= 0) {

                    removeFromCart(
                            session,
                            productId
                    );

                    return;
                }

                item.setQuantity(quantity);

                break;
            }
        }

        session.setAttribute(
                CART_SESSION_KEY,
                cart
        );
    }


    public void removeFromCart(HttpSession session,
                               Long productId) {

        List<CartItem> cart = getCart(session);

        cart.removeIf(
                item -> item.getProduct()
                        .getId()
                        .equals(productId)
        );

        session.setAttribute(
                CART_SESSION_KEY,
                cart
        );
    }


    public void clearCart(HttpSession session) {

        session.removeAttribute(
                CART_SESSION_KEY
        );
    }


    public double getTotal(HttpSession session) {

        List<CartItem> cart = getCart(session);

        double total = 0;

        for (CartItem item : cart) {

            total += item.getSubtotal();
        }

        return total;
    }


    public int getTotalQuantity(HttpSession session) {

        List<CartItem> cart = getCart(session);

        int totalQuantity = cart.size();

        return totalQuantity;
    }
}