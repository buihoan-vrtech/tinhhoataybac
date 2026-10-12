package vn.edu.crs.tinhhoataybac.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.model.CartItem;
import vn.edu.crs.tinhhoataybac.model.Product;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final vn.edu.crs.tinhhoataybac.repository.ProductRepository products;
    public CartService(vn.edu.crs.tinhhoataybac.repository.ProductRepository products) { this.products = products; }

    private static final String CART_SESSION_KEY = "cart";


    @SuppressWarnings("unchecked")
    public List<CartItem> getCart(HttpSession session) {

        Object cartObject = session.getAttribute(CART_SESSION_KEY);

        if (cartObject == null) {

            List<CartItem> newCart = new ArrayList<>();

            session.setAttribute(CART_SESSION_KEY, newCart);

            return newCart;
        }

        List<CartItem> cart = (List<CartItem>) cartObject;
        var ids = cart.stream().map(i -> i.getProduct().getId()).distinct().toList();
        var current = products.findAllById(ids).stream().collect(java.util.stream.Collectors.toMap(Product::getId, java.util.function.Function.identity()));
        for (CartItem item : cart) {
            Product fresh = current.get(item.getProduct().getId());
            if (fresh != null) item.setProduct(fresh);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }


    public void addToCart(HttpSession session,
                          Product product,
                          double quantity) {

        product = products.findById(product.getId()).orElseThrow(() -> new IllegalStateException("Sản phẩm không còn tồn tại."));
        product.validateQuantity(quantity);
        List<CartItem> cart = getCart(session);
        if(product.getStock()==null||product.getStock()<=0||quantity<=0)throw new IllegalStateException("Sản phẩm đã hết hàng.");
        if (quantity > product.getStock()) throw new IllegalStateException("Không đủ tồn kho.");
        for (CartItem item : cart) {

            if (item.getProduct().getId().equals(product.getId())) {

                double combined = Math.round((item.getQuantity() + quantity)*100.0)/100.0;
                product.validateQuantity(combined);
                if (combined > product.getStock()) throw new IllegalStateException("Tổng số lượng trong giỏ vượt tồn kho.");
                item.setProduct(product);
                item.setQuantity(combined);

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

                Product fresh = item.getProduct();
                fresh.validateQuantity(quantity);
                if (fresh.getStock() == null || quantity > fresh.getStock()) throw new IllegalStateException("Không đủ tồn kho.");
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