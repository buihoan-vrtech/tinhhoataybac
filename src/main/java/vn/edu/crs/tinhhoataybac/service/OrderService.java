package vn.edu.crs.tinhhoataybac.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import vn.edu.crs.tinhhoataybac.model.CartItem;
import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.model.OrderDetail;
import vn.edu.crs.tinhhoataybac.model.OrderStatusHistory;
import vn.edu.crs.tinhhoataybac.model.Product;
import vn.edu.crs.tinhhoataybac.model.User;

import vn.edu.crs.tinhhoataybac.repository.OrderRepository;
import vn.edu.crs.tinhhoataybac.repository.OrderStatusHistoryRepository;
import vn.edu.crs.tinhhoataybac.repository.ProductRepository;
import vn.edu.crs.tinhhoataybac.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;


@Service
public class OrderService {

    private final NotificationService notifications;
    private final OrderStatusHistoryRepository histories;
    private final UserRepository users;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private final UserService userService;
    private final WalletService walletService;


    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            NotificationService notifications,
            OrderStatusHistoryRepository histories,
            UserRepository users,
            UserService userService,
            WalletService walletService) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;

        this.notifications = notifications;
        this.histories = histories;
        this.users = users;

        this.userService = userService;
        this.walletService = walletService;
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

        order.setOrderStatus("PENDING");


        /*
         * QR phải chờ SePay xác nhận.
         */
        if ("QR".equalsIgnoreCase(paymentMethod)) {

            order.setPaymentStatus("PENDING");

        } else {

            /*
             * COD:
             * chưa thanh toán.
             *
             * WALLET:
             * được chuyển sang PAID ngay sau
             * khi walletService.pay() thành công.
             */
            order.setPaymentStatus("UNPAID");
        }


        BigDecimal total = BigDecimal.ZERO;


        /*
         * Sắp xếp theo product ID trước khi lock.
         */
        List<CartItem> sortedItems =
                cartItems.stream()
                        .sorted(
                                Comparator.comparing(
                                        item ->
                                                item.getProduct()
                                                        .getId()
                                )
                        )
                        .toList();


        for (CartItem cartItem : sortedItems) {

            Product product =
                    productRepository
                            .lockById(
                                    cartItem
                                            .getProduct()
                                            .getId()
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
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
                            BigDecimal.valueOf(quantity)
                    );


            /*
             * Chia theo lô hàng nếu sản phẩm
             * đang sử dụng batch.
             */
            for (var allocation :
                    BatchAllocator.allocate(
                            product,
                            quantity
                    )) {

                OrderDetail detail =
                        new OrderDetail();

                detail.setProduct(product);

                detail.setQuantity(
                        allocation.quantity()
                );

                detail.setUnitPrice(
                        unitPrice
                );

                detail.setSubtotal(
                        unitPrice.multiply(
                                BigDecimal.valueOf(
                                        allocation.quantity()
                                )
                        )
                );

                detail.setBatch(
                        allocation.batch()
                );

                detail.setVariantSnapshot(
                        product.getVariantLabel()
                );

                order.addOrderDetail(
                        detail
                );
            }


            total =
                    total.add(subtotal);


            /*
             * Sản phẩm không quản lý theo batch
             * thì trừ trực tiếp stock.
             */
            if (!product.getBatchTracked()) {

                product.setStock(
                        Math.round(
                                (
                                        product.getStock()
                                                - quantity
                                ) * 100.0
                        ) / 100.0
                );
            }


            productRepository.save(product);
        }


        order.setTotalAmount(total);


        /*
         * Lưu để lấy ID.
         */
        Order savedOrder =
                orderRepository.save(order);


        /*
         * THB1, THB2...
         */
        savedOrder.setPaymentCode(
                "THB" + savedOrder.getId()
        );


        return orderRepository.save(
                savedOrder
        );
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
     * SEPAY XÁC NHẬN QR
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


        /*
         * Đơn đã hủy hoặc đã hoàn tiền
         * thì không được xác nhận thanh toán lại.
         */
        if ("CANCELLED".equalsIgnoreCase(
                order.getOrderStatus()
        )
                || "REFUNDED".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return false;
        }


        /*
         * Đã thanh toán.
         */
        if ("PAID".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return true;
        }


        /*
         * Chỉ áp dụng cho QR.
         */
        if (!"QR".equalsIgnoreCase(
                order.getPaymentMethod()
        )) {

            return false;
        }


        /*
         * Tiền phải khớp chính xác.
         */
        if (receivedAmount == null
                || receivedAmount.compareTo(
                order.getTotalAmount()
        ) != 0) {

            return false;
        }


        /*
         * QR hết hạn.
         */
        if (order.getPaymentExpiresAt() != null
                && !order.getPaymentExpiresAt()
                .isAfter(
                        LocalDateTime.now()
                )) {

            return false;
        }


        order.setPaymentStatus(
                "PAID"
        );

        order.setOrderStatus(
                "CONFIRMED"
        );

        order.setPaidAt(
                LocalDateTime.now()
        );


        orderRepository.save(order);


        saveHistory(
                order,
                "Thanh toán thành công",
                "Hệ thống"
        );


        notifyUser(
                order,
                "Đơn #"
                        + order.getId()
                        + ": thanh toán thành công."
        );


        return true;
    }


    /*
     * =========================================================
     * THANH TOÁN BẰNG VÍ
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


        if ("CANCELLED".equalsIgnoreCase(
                order.getOrderStatus()
        )
                || "REFUNDED".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return false;
        }


        if (!"WALLET".equalsIgnoreCase(
                order.getPaymentMethod()
        )) {

            return false;
        }


        if ("PAID".equalsIgnoreCase(
                order.getPaymentStatus()
        )) {

            return true;
        }


        order.setPaymentStatus(
                "PAID"
        );

        order.setOrderStatus(
                "CONFIRMED"
        );

        order.setPaidAt(
                LocalDateTime.now()
        );


        orderRepository.save(order);


        saveHistory(
                order,
                "Thanh toán thành công",
                "Hệ thống"
        );


        notifyUser(
                order,
                "Đơn #"
                        + order.getId()
                        + ": thanh toán thành công."
        );


        return true;
    }


    /*
     * =========================================================
     * KIỂM TRA KHÁCH CÒN ĐƯỢC TỰ HỦY KHÔNG
     * =========================================================
     *
     * Chỉ cho tự hủy trong vòng 1 giờ đầu.
     */
    public boolean canUserCancel(
            Order order) {

        if (order == null) {
            return false;
        }


        if (order.getCreatedAt() == null) {
            return false;
        }


        String status =
                order.getOrderStatus();


        /*
         * Những trạng thái này không được hủy.
         */
        if ("CANCELLED".equalsIgnoreCase(status)
                || "SHIPPING".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)) {

            return false;
        }


        LocalDateTime deadline =
                order.getCreatedAt()
                        .plusHours(1);


        return LocalDateTime.now()
                .isBefore(deadline);
    }


    /*
     * =========================================================
     * KHÁCH HÀNG TỰ HỦY ĐƠN TRONG 1 GIỜ
     * =========================================================
     *
     * COD:
     * - hủy
     * - trả tồn kho
     *
     * QR/WALLET đã PAID:
     * - hủy
     * - trả tồn kho
     * - hoàn tiền về Ví Tinh Hoa
     * - paymentStatus = REFUNDED
     *
     * Chống hoàn tiền 2 lần bằng field refunded.
     */
    @Transactional
    public Order cancelOrderByUser(
            Long orderId,
            String userEmail,
            String reason) {

        if (orderId == null) {

            throw new IllegalStateException(
                    "Mã đơn hàng không hợp lệ."
            );
        }


        if (userEmail == null
                || userEmail.isBlank()) {

            throw new IllegalStateException(
                    "Người dùng không hợp lệ."
            );
        }


        /*
         * Chỉ lấy đơn thuộc email đang đăng nhập.
         */
        Order order =
                orderRepository
                        .findByIdAndEmailIgnoreCase(
                                orderId,
                                userEmail.trim()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Không tìm thấy đơn hàng."
                                        )
                        );


        /*
         * Không cho hủy lần 2.
         */
        if ("CANCELLED".equalsIgnoreCase(
                order.getOrderStatus()
        )) {

            throw new IllegalStateException(
                    "Đơn hàng đã được hủy trước đó."
            );
        }


        /*
         * Quá 1 giờ.
         */
        if (!canUserCancel(order)) {

            throw new IllegalStateException(
                    "Đơn hàng đã quá thời gian tự hủy 1 giờ."
            );
        }


        /*
         * =====================================================
         * TRẢ TỒN KHO
         * =====================================================
         */
        for (OrderDetail detail :
                order.getOrderDetails()) {

            Product product =
                    detail.getProduct();


            if (product == null) {
                continue;
            }


            /*
             * Với sản phẩm KHÔNG quản lý batch,
             * cộng lại stock trực tiếp.
             */
            if (!product.getBatchTracked()) {

                double currentStock =
                        product.getStock() == null
                                ? 0.0
                                : product.getStock();


                double restored =
                        currentStock
                                + detail.getQuantity();


                product.setStock(
                        Math.round(
                                restored * 100.0
                        ) / 100.0
                );


                productRepository.save(
                        product
                );
            }

            /*
             * Nếu product có batch:
             *
             * Không cộng product.stock ở đây vì lúc đặt hàng
             * BatchAllocator đã xử lý theo từng lô.
             *
             * Phần hoàn số lượng về batch cần xử lý bằng
             * service/repository batch riêng của project.
             */
        }


        /*
         * =====================================================
         * KIỂM TRA CÓ PHẢI THANH TOÁN ONLINE KHÔNG
         * =====================================================
         */
        boolean onlinePayment =
                "QR".equalsIgnoreCase(
                        order.getPaymentMethod()
                )
                        ||
                        "WALLET".equalsIgnoreCase(
                                order.getPaymentMethod()
                        );


        boolean paid =
                "PAID".equalsIgnoreCase(
                        order.getPaymentStatus()
                );


        boolean alreadyRefunded =
                Boolean.TRUE.equals(
                        order.getRefunded()
                );


        /*
         * =====================================================
         * HOÀN TIỀN VỀ VÍ
         * =====================================================
         */
        if (onlinePayment
                && paid
                && !alreadyRefunded) {

            /*
             * Lấy user theo email của đơn.
             */
            User user =
                    userService.findByEmail(
                            order.getEmail()
                    );


            if (user == null) {

                throw new IllegalStateException(
                        "Không tìm thấy tài khoản nhận tiền hoàn."
                );
            }


            walletService.refund(
                    user,
                    order.getTotalAmount(),
                    "Hoàn tiền đơn hàng #"
                            + order.getId(),
                    order.getPaymentCode()
            );


            /*
             * Đánh dấu đã hoàn tiền.
             */
            order.setRefunded(
                    true
            );

            order.setRefundedAt(
                    LocalDateTime.now()
            );


            /*
             * Tiền đã hoàn về ví.
             */
            order.setPaymentStatus(
                    "REFUNDED"
            );
        }


        /*
         * =====================================================
         * ĐÁNH DẤU ĐƠN ĐÃ HỦY
         * =====================================================
         */
        order.setOrderStatus(
                "CANCELLED"
        );


        order.setCancelledAt(
                LocalDateTime.now()
        );


        if (reason == null
                || reason.isBlank()) {

            order.setCancelReason(
                    "Khách hàng tự hủy trong vòng 1 giờ."
            );

        } else {

            order.setCancelReason(
                    reason.trim()
            );
        }


        Order saved =
                orderRepository.save(order);


        /*
         * Lưu lịch sử.
         */
        saveHistory(
                saved,
                "Khách hàng hủy đơn"
                        + (
                        saved.getCancelReason() == null
                                ? ""
                                : ": "
                                + saved.getCancelReason()
                ),
                "Khách hàng"
        );


        /*
         * Thông báo.
         */
        if (Boolean.TRUE.equals(
                saved.getRefunded()
        )) {

            notifyUser(
                    saved,
                    "Đơn #"
                            + saved.getId()
                            + " đã được hủy. "
                            + "Tiền đã được hoàn về Ví Tinh Hoa."
            );

        } else {

            notifyUser(
                    saved,
                    "Đơn #"
                            + saved.getId()
                            + " đã được hủy thành công."
            );
        }


        return saved;
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


    /*
     * =========================================================
     * ADMIN - TOÀN BỘ ĐƠN
     * =========================================================
     */
    public List<Order> getAllOrders() {

        return orderRepository
                .findAll()
                .stream()
                .sorted(
                        (a, b) -> {

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
                                    .compareTo(
                                            a.getCreatedAt()
                                    );
                        }
                )
                .toList();
    }


    /*
     * =========================================================
     * ADMIN - CẬP NHẬT TRẠNG THÁI
     * =========================================================
     */
    @Transactional
    public Order updateOrderStatus(
            Long orderId,
            String newStatus) {

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
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


        order.setOrderStatus(
                status
        );


        Order saved =
                orderRepository.save(order);


        saveHistory(
                saved,
                "Cập nhật trạng thái đơn hàng",
                "Quản trị viên"
        );


        notifyUser(
                saved,
                "Đơn #"
                        + saved.getId()
                        + " chuyển sang trạng thái "
                        + saved.getOrderStatus()
                        + "."
        );


        return saved;
    }


    /*
     * =========================================================
     * HÀM DÙNG CHUNG - LƯU LỊCH SỬ
     * =========================================================
     */
    private void saveHistory(
            Order order,
            String note,
            String actor) {

        OrderStatusHistory history =
                new OrderStatusHistory();


        history.setOrder(
                order
        );

        history.setStatus(
                order.getOrderStatus()
        );

        history.setNote(
                note
        );

        history.setActor(
                actor
        );


        histories.save(
                history
        );
    }


    /*
     * =========================================================
     * HÀM DÙNG CHUNG - GỬI THÔNG BÁO
     * =========================================================
     */
    private void notifyUser(
            Order order,
            String message) {

        User user = null;


        /*
         * Nếu Order đã có customer thì dùng luôn.
         */
        if (order.getCustomer() != null) {

            user =
                    order.getCustomer();

        } else if (order.getEmail() != null) {

            /*
             * Nếu chưa có quan hệ customer,
             * tìm theo email.
             */
            user =
                    users.findByEmail(
                                    order.getEmail()
                            )
                            .orElse(null);
        }


        if (user != null) {

            notifications.notify(
                    user,
                    order.getId(),
                    message
            );
        }
    }
}