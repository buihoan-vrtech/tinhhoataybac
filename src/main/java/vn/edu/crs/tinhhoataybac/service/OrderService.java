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
import java.time.LocalDateTime;
import java.util.List;


@Service
public class OrderService {

    private final NotificationService notifications;
    private final vn.edu.crs.tinhhoataybac.repository.OrderStatusHistoryRepository histories;
    private final vn.edu.crs.tinhhoataybac.repository.UserRepository users;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;


    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,NotificationService notifications,
            vn.edu.crs.tinhhoataybac.repository.OrderStatusHistoryRepository histories,
            vn.edu.crs.tinhhoataybac.repository.UserRepository users) {
        this.notifications=notifications;this.histories=histories;this.users=users;

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }


    /*
     * =========================================================
     * TẠO ĐƠN HÀNG
     * =========================================================
     */
    @Transactional
    public Order createOrder(
            String customerName,
            String phone,
            String email,
            String address,
            String note,
            String paymentMethod,
            List<CartItem> cartItems) {

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalStateException(
                    "Giỏ hàng đang trống."
            );
        }


        Order order = new Order();

        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setEmail(email);
        order.setAddress(address);
        order.setNote(note);

        order.setPaymentMethod(paymentMethod);

        /*
         * Trạng thái mặc định của đơn.
         */
        order.setOrderStatus("PENDING");


        /*
         * QR:
         * đang chờ SePay xác nhận.
         */
        if ("QR".equalsIgnoreCase(paymentMethod)) {

            order.setPaymentStatus("PENDING");

        } else {

            /*
             * COD và WALLET:
             *
             * WALLET sẽ được chuyển PAID
             * ngay sau khi trừ tiền thành công.
             */
            order.setPaymentStatus("UNPAID");
        }


        BigDecimal total = BigDecimal.ZERO;


        /*
         * =====================================================
         * DUYỆT GIỎ HÀNG
         * =====================================================
         */
        for (CartItem cartItem : cartItems.stream().sorted(java.util.Comparator.comparing(i -> i.getProduct().getId())).toList()) {

            Product product =
                    productRepository
                            .lockById(
                                    cartItem
                                            .getProduct()
                                            .getId()
                            )
                            .orElseThrow(
                                    () -> new IllegalStateException(
                                            "Sản phẩm không tồn tại."
                                    )
                            );


            double quantity =
                    cartItem.getQuantity();


            product.validateQuantity(quantity);
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
                    BigDecimal.valueOf(
                            product.getEffectivePrice()
                    );


            BigDecimal subtotal =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    quantity
                            )
                    );


            OrderDetail detail =
                    new OrderDetail();


            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unitPrice);
            detail.setSubtotal(subtotal);


            /*
             * addOrderDetail sẽ tự set
             * detail.setOrder(order)
             * nếu model Order của bạn đang đúng như trước.
             */
            order.addOrderDetail(detail);


            total =
                    total.add(subtotal);


            /*
             * Trừ tồn kho.
             */
            product.setStock(
                    Math.round((product.getStock() - quantity)*100.0)/100.0
            );


            productRepository.save(product);
        }


        order.setTotalAmount(total);


        /*
         * Lưu lần đầu để lấy ID.
         */
        Order savedOrder =
                orderRepository.save(order);


        /*
         * Sinh mã thanh toán:
         *
         * THB1
         * THB2
         * THB3
         */
        savedOrder.setPaymentCode(
                "THB" + savedOrder.getId()
        );


        return orderRepository.save(savedOrder);
    }


    /*
     * =========================================================
     * LẤY ĐƠN THEO ID
     * =========================================================
     */
    public Order getOrderById(Long id) {

        if (id == null) {
            return null;
        }


        return orderRepository
                .findById(id)
                .orElse(null);
    }


    /*
     * =========================================================
     * SEPAY XÁC NHẬN THANH TOÁN QR
     * =========================================================
     */
    @Transactional
    public boolean markOrderPaid(
            String paymentCode,
            BigDecimal receivedAmount) {

        Order order =
                orderRepository
                        .lockByPaymentCode(
                                paymentCode
                        )
                        .orElse(null);


        if (order == null) {
            return false;
        }
        if ("CANCELLED".equals(order.getOrderStatus()) || "REFUNDED".equals(order.getPaymentStatus())) return false;


        /*
         * Đã PAID rồi thì không xử lý lần nữa.
         */
        if ("PAID".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return true;
        }


        /*
         * Hàm này chỉ dành cho QR.
         */
        if (!"QR".equalsIgnoreCase(
                order.getPaymentMethod()
        )) {

            return false;
        }


        /*
         * Kiểm tra số tiền.
         */
        if (receivedAmount == null
                || receivedAmount.compareTo(
                order.getTotalAmount()
        ) < 0) {

            return false;
        }


        if (order.getPaymentExpiresAt()!=null && !order.getPaymentExpiresAt().isAfter(LocalDateTime.now())) return false;
        order.setPaymentStatus("PAID");

        order.setOrderStatus("CONFIRMED");

        order.setPaidAt(
                LocalDateTime.now()
        );


        orderRepository.save(order);
        var h=new vn.edu.crs.tinhhoataybac.model.OrderStatusHistory();h.setOrder(order);h.setStatus(order.getOrderStatus());h.setNote("Thanh toán thành công");h.setActor("Hệ thống");histories.save(h);
        notifications.notify(order.getCustomer()!=null?order.getCustomer():users.findByEmail(order.getEmail()).orElse(null),order.getId(),"Đơn #"+order.getId()+": thanh toán thành công.");
        return true;
    }


    /*
     * =========================================================
     * ĐÁNH DẤU ĐƠN THANH TOÁN BẰNG VÍ
     * =========================================================
     *
     * Hàm này được CheckoutController gọi
     * sau khi walletService.pay(...) thành công.
     * =========================================================
     */
    @Transactional
    public boolean markWalletOrderPaid(
            Long orderId) {

        if (orderId == null) {
            return false;
        }


        Order order =
                orderRepository
                        .findById(orderId)
                        .orElse(null);


        if (order == null) {
            return false;
        }
        if ("CANCELLED".equals(order.getOrderStatus()) || "REFUNDED".equals(order.getPaymentStatus())) return false;


        /*
         * Chỉ cho đơn WALLET.
         */
        if (!"WALLET".equalsIgnoreCase(
                order.getPaymentMethod()
        )) {

            return false;
        }


        /*
         * Nếu đã PAID rồi thì coi như thành công.
         */
        if ("PAID".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return true;
        }


        order.setPaymentStatus("PAID");

        order.setOrderStatus("CONFIRMED");

        order.setPaidAt(
                LocalDateTime.now()
        );


        orderRepository.save(order);
        var h=new vn.edu.crs.tinhhoataybac.model.OrderStatusHistory();h.setOrder(order);h.setStatus(order.getOrderStatus());h.setNote("Thanh toán thành công");h.setActor("Hệ thống");histories.save(h);
        notifications.notify(order.getCustomer()!=null?order.getCustomer():users.findByEmail(order.getEmail()).orElse(null),order.getId(),"Đơn #"+order.getId()+": thanh toán thành công.");
        return true;
    }


    /*
     * =========================================================
     * LỊCH SỬ ĐƠN HÀNG THEO EMAIL
     * =========================================================
     */
    public List<Order> getOrdersByEmail(
            String email) {

        if (email == null
                || email.trim().isEmpty()) {

            return List.of();
        }


        return orderRepository
                .findByEmailIgnoreCaseOrderByCreatedAtDesc(
                        email.trim()
                );
    }


    /*
     * =========================================================
     * LẤY ĐƠN THUỘC USER
     * =========================================================
     */
    public Order getOrderForUser(
            Long orderId,
            String email) {

        if (orderId == null
                || email == null
                || email.trim().isEmpty()) {

            return null;
        }


        return orderRepository
                .findByIdAndEmailIgnoreCase(
                        orderId,
                        email.trim()
                )
                .orElse(null);
    }
    public List<Order> getAllOrders() {

        return orderRepository
                .findAll()
                .stream()
                .sorted((a, b) -> {

                    if (a.getCreatedAt() == null
                            && b.getCreatedAt() == null) {
                        return 0;
                    }

                    if (a.getCreatedAt() == null) {
                        return 1;
                    }

                    if (b.getCreatedAt() == null) {
                        return -1;
                    }

                    return b.getCreatedAt()
                            .compareTo(a.getCreatedAt());
                })
                .toList();
    }


    @Transactional
    public Order updateOrderStatus(
            Long orderId,
            String newStatus) {

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Không tìm thấy đơn hàng."
                                )
                        );


        if (newStatus == null) {

            throw new IllegalStateException(
                    "Trạng thái không hợp lệ."
            );
        }


        String status =
                newStatus
                        .trim()
                        .toUpperCase();


        if (!status.equals("PENDING")
                && !status.equals("CONFIRMED")
                && !status.equals("SHIPPING")
                && !status.equals("COMPLETED")
                && !status.equals("CANCELLED")) {

            throw new IllegalStateException(
                    "Trạng thái đơn hàng không hợp lệ."
            );
        }


        order.setOrderStatus(status);


        return orderRepository.save(order);
    }
}