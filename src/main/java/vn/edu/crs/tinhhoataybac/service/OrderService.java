package vn.edu.crs.tinhhoataybac.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.model.CartItem;
import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.model.OrderDetail;
import vn.edu.crs.tinhhoataybac.model.Product;
import vn.edu.crs.tinhhoataybac.repository.OrderRepository;
import vn.edu.crs.tinhhoataybac.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order createOrder(String customerName,
                             String phone,
                             String email,
                             String address,
                             String note,
                             String paymentMethod,
                             List<CartItem> cartItems) {

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống.");
        }

        Order order = new Order();

        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setEmail(email);
        order.setAddress(address);
        order.setNote(note);

        order.setPaymentMethod(paymentMethod);
        order.setOrderStatus("PENDING");

        if ("QR".equalsIgnoreCase(paymentMethod)) {

            order.setPaymentStatus("PENDING");

        } else {

            order.setPaymentStatus("UNPAID");
        }

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {

            Product product = productRepository
                    .findById(cartItem.getProduct().getId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Sản phẩm không tồn tại."
                            )
                    );

            int quantity = cartItem.getQuantity();

            if (quantity <= 0) {
                throw new IllegalStateException(
                        "Số lượng sản phẩm không hợp lệ."
                );
            }

            if (product.getStock() == null
                    || product.getStock() < quantity) {

                throw new IllegalStateException(
                        "Sản phẩm "
                                + product.getName()
                                + " không đủ số lượng trong kho."
                );
            }

            BigDecimal unitPrice =
                    BigDecimal.valueOf(product.getPrice());

            BigDecimal subtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(quantity)
                    );

            OrderDetail detail = new OrderDetail();

            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unitPrice);
            detail.setSubtotal(subtotal);

            order.addOrderDetail(detail);

            total = total.add(subtotal);

            product.setStock(
                    product.getStock() - quantity
            );

            productRepository.save(product);
        }

        order.setTotalAmount(total);

        Order savedOrder =
                orderRepository.save(order);

        savedOrder.setPaymentCode(
                "THB" + savedOrder.getId()
        );

        return orderRepository.save(savedOrder);
    }

    public Order getOrderById(Long id) {

        return orderRepository
                .findById(id)
                .orElse(null);
    }
}