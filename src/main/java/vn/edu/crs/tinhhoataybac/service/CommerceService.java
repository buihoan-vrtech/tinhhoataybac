package vn.edu.crs.tinhhoataybac.service;

import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;

@Service
public class CommerceService {
  private final ShipmentEventRepository shipmentEvents;
  private final OrderService orders;
  private final OrderRepository orderRepo;
  private final ProductRepository products;
  private final VoucherRepository vouchers;
  private final VoucherService voucherService;
  private final WalletService wallets;
  private final UserService users;
  private final OrderStatusHistoryRepository histories;
  private final CustomerNotificationRepository notifications;
  private final OrderServiceRequestRepository requests;

  public CommerceService(
      OrderService orders,
      OrderRepository orderRepo,
      ProductRepository products,
      VoucherRepository vouchers,
      VoucherService voucherService,
      WalletService wallets,
      UserService users,
      OrderStatusHistoryRepository histories,
      CustomerNotificationRepository notifications,
      OrderServiceRequestRepository requests,
      ShipmentEventRepository shipmentEvents) {
    this.shipmentEvents = shipmentEvents;
    this.orders = orders;
    this.orderRepo = orderRepo;
    this.products = products;
    this.vouchers = vouchers;
    this.voucherService = voucherService;
    this.wallets = wallets;
    this.users = users;
    this.histories = histories;
    this.notifications = notifications;
    this.requests = requests;
  }

  public BigDecimal shippingFee(String method) {
    return switch (method) {
      case "standard" -> new BigDecimal("25000");
      case "fast" -> new BigDecimal("35000");
      case "express" -> new BigDecimal("50000");
      case "pickup" -> BigDecimal.ZERO;
      default -> throw new IllegalStateException("Phương thức giao hàng không hợp lệ.");
    };
  }

  @Transactional
  public Order checkout(
      User user,
      String name,
      String phone,
      String address,
      String note,
      String payment,
      List<CartItem> items,
      String shipping,
      String code) {
    if (name == null
        || name.isBlank()
        || name.length() > 255
        || phone == null
        || !phone.matches("[+0-9 ()-]{8,20}")
        || address == null
        || address.isBlank()
        || address.length() > 500)
      throw new IllegalStateException("Thông tin người nhận không hợp lệ.");
    if (!Set.of("COD", "QR", "WALLET").contains(payment))
      throw new IllegalStateException("Phương thức thanh toán không hợp lệ.");
    BigDecimal fee = shippingFee(shipping);
    Order order =
        orders.createOrder(
            name.trim(), phone.trim(), user.getEmail(), address.trim(), note, payment, items);
    BigDecimal subtotal = order.getTotalAmount(), discount = BigDecimal.ZERO;
    if (code != null && !code.isBlank()) {
      Voucher v =
          vouchers
              .lockByCode(code.trim())
              .orElseThrow(() -> new IllegalStateException("Không tìm thấy voucher."));
      discount = voucherService.discount(v, subtotal, fee);
      v.setUsedCount(v.getUsedCount() + 1);
      vouchers.save(v);
      order.setVoucherCode(v.getCode());
    }
    order.setCustomer(user);
    order.setSubtotal(subtotal);
    order.setShippingFee(fee);
    order.setShippingMethod(shipping);
    order.setDiscount(discount);
    order.setTotalAmount(subtotal.add(fee).subtract(discount));
    if ("QR".equals(payment)) order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(5));
    orderRepo.save(order);
    if ("WALLET".equals(payment)) {
      if (order.getTotalAmount().signum() > 0)
        wallets.pay(
            user,
            order.getTotalAmount(),
            "Thanh toán đơn #" + order.getId(),
            order.getPaymentCode());
      orders.markWalletOrderPaid(order.getId());
    }
    record(order, "Đặt hàng thành công", user.getEmail());
    return order;
  }

  public User customer(Order order) {
    return order.getCustomer() != null ? order.getCustomer() : users.findByEmail(order.getEmail());
  }

  public boolean owns(Order order, User user) {
    return order != null
        && user != null
        && (order.getCustomer() != null
            ? order.getCustomer().getId().equals(user.getId())
            : user.getEmail().equalsIgnoreCase(order.getEmail()));
  }

  public void record(Order order, String note, String actor) {
    OrderStatusHistory h = new OrderStatusHistory();
    h.setOrder(order);
    h.setStatus(order.getOrderStatus());
    h.setNote(note);
    h.setActor(actor);
    histories.save(h);
    User u = customer(order);
    if (u != null) {
      CustomerNotification n = new CustomerNotification();
      n.setUser(u);
      n.setOrderId(order.getId());
      n.setMessage("Đơn #" + order.getId() + ": " + note);
      notifications.save(n);
    }
  }

  @Transactional
  public Order changeStatus(Long id, String status, String actor) {
    Order o =
        orderRepo
            .lockById(id)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng."));
    status = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("PENDING", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELLED").contains(status))
      throw new IllegalStateException("Trạng thái không hợp lệ.");
    String old = o.getOrderStatus();
    if (status.equals(old)) return o;
    if (Set.of("CANCELLED", "COMPLETED").contains(old))
      throw new IllegalStateException("Đơn đã kết thúc, không thể đổi trạng thái.");
    if ("CANCELLED".equals(status)) {
      if ("SHIPPING".equals(old))
        throw new IllegalStateException("Đơn đang vận chuyển, hãy xử lý yêu cầu hoàn hàng.");
      for (OrderDetail d :
          o.getOrderDetails().stream()
              .sorted(Comparator.comparing(d -> d.getProduct().getId()))
              .toList()) {
        Product p = products.lockById(d.getProduct().getId()).orElseThrow();
        p.setStock((p.getStock() == null ? 0 : p.getStock()) + d.getQuantity());
        products.save(p);
      }
      refund(o);
      if (o.getVoucherCode() != null)
        vouchers
            .lockByCode(o.getVoucherCode())
            .ifPresent(
                v -> {
                  v.setUsedCount(Math.max(0, v.getUsedCount() - 1));
                  vouchers.save(v);
                });
    } else {
      Map<String, String> next =
          Map.of("PENDING", "CONFIRMED", "CONFIRMED", "SHIPPING", "SHIPPING", "COMPLETED");
      if (!status.equals(next.get(old)))
        throw new IllegalStateException(
            "Vui lòng cập nhật theo thứ tự: chờ xác nhận → xác nhận → vận chuyển → hoàn tất.");
      if (!"COD".equals(o.getPaymentMethod()) && !"PAID".equals(o.getPaymentStatus()))
        throw new IllegalStateException("Đơn cần thanh toán trước khi xử lý.");
      if ("COMPLETED".equals(status) && "COD".equals(o.getPaymentMethod())) {
        o.setPaymentStatus("PAID");
        o.setPaidAt(LocalDateTime.now());
      }
    }
    o.setOrderStatus(status);
    orderRepo.save(o);
    record(o, "Trạng thái: " + status, actor);
    return o;
  }

  private void refund(Order o) {
    if ("PAID".equals(o.getPaymentStatus())) {
      User u = customer(o);
      if (u == null) throw new IllegalStateException("Không tìm thấy tài khoản nhận hoàn tiền.");
      if (o.getTotalAmount().signum() > 0)
        wallets.deposit(
            u, o.getTotalAmount(), "Hoàn tiền đơn #" + o.getId(), "REFUND-" + o.getId());
      o.setPaymentStatus("REFUNDED");
    }
  }

  @Transactional
  public void request(Long id, User user, String type, String reason) {
    Order o = orderRepo.lockById(id).orElseThrow();
    if (!owns(o, user)) throw new IllegalStateException("Bạn không có quyền với đơn này.");
    if (reason == null || reason.trim().length() < 10 || reason.length() > 1000)
      throw new IllegalStateException("Lý do cần từ 10 đến 1000 ký tự.");
    if (requests.existsByOrderIdAndStatus(id, "PENDING"))
      throw new IllegalStateException("Đã có yêu cầu đang chờ xử lý.");
    if ("cancel".equals(type)) {
      if (!Set.of("PENDING", "CONFIRMED").contains(o.getOrderStatus()))
        throw new IllegalStateException("Chỉ được yêu cầu hủy trước khi vận chuyển.");
    } else if ("refund".equals(type)) {
      if (!"COMPLETED".equals(o.getOrderStatus()) || !"PAID".equals(o.getPaymentStatus()))
        throw new IllegalStateException("Chỉ hoàn tiền đơn đã giao và thanh toán.");
    } else throw new IllegalStateException("Loại yêu cầu không hợp lệ.");
    OrderServiceRequest r = new OrderServiceRequest();
    r.setOrder(o);
    r.setUser(user);
    r.setType(type);
    r.setReason(reason.trim());
    requests.save(r);
  }

  @Transactional
  public void processRequest(Long id, String decision, String note, String actor) {
    OrderServiceRequest r = requests.lockById(id).orElseThrow();
    if (!"PENDING".equals(r.getStatus())) throw new IllegalStateException("Yêu cầu đã được xử lý.");
    if (!Set.of("APPROVED", "REJECTED").contains(decision))
      throw new IllegalStateException("Quyết định không hợp lệ.");
    if (note != null && note.length() > 1000) throw new IllegalStateException("Ghi chú quá dài.");
    Order o = orderRepo.lockById(r.getOrder().getId()).orElseThrow();
    if ("APPROVED".equals(decision)) {
      if ("cancel".equals(r.getType())) changeStatus(o.getId(), "CANCELLED", actor);
      else {
        if (!"COMPLETED".equals(o.getOrderStatus()) || !"PAID".equals(o.getPaymentStatus()))
          throw new IllegalStateException("Đơn không còn đủ điều kiện hoàn tiền.");
        refund(o);
        orderRepo.save(o);
      }
    }
    r.setStatus(decision);
    r.setAdminNote(note);
    r.setProcessedAt(LocalDateTime.now());
    requests.save(r);
    record(o, "Yêu cầu " + r.getType() + ": " + decision, actor);
  }

  @Transactional
  public void shipment(Long id, String carrier, String tracking, String note, String actor) {
    Order o = orderRepo.lockById(id).orElseThrow();
    if (Set.of("CANCELLED", "COMPLETED").contains(o.getOrderStatus()))
      throw new IllegalStateException("Đơn đã kết thúc.");
    if (carrier == null
        || carrier.isBlank()
        || carrier.length() > 255
        || tracking == null
        || tracking.isBlank()
        || tracking.length() > 255)
      throw new IllegalStateException("Nhập đơn vị vận chuyển và mã vận đơn.");
    o.setShippingCarrier(carrier.trim());
    o.setTrackingCode(tracking.trim());
    orderRepo.save(o);
    record(
        o, "Vận chuyển: " + carrier + " / " + tracking + (note == null ? "" : ". " + note), actor);
  }

  @Transactional
  public void expirePendingPayments() {
    for (Order candidate : orderRepo.findAll()) {
      if ("QR".equals(candidate.getPaymentMethod())
          && !"PAID".equals(candidate.getPaymentStatus())
          && "PENDING".equals(candidate.getOrderStatus())
          && candidate.getPaymentExpiresAt() != null
          && !candidate.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
        Order o = orderRepo.lockById(candidate.getId()).orElseThrow();
        if ("PENDING".equals(o.getOrderStatus()) && !"PAID".equals(o.getPaymentStatus()))
          changeStatus(o.getId(), "CANCELLED", "Hết thời gian thanh toán");
      }
    }
  }

  public Map<String, BigDecimal> quote(List<CartItem> items, String shipping, String code) {
    if (items == null || items.isEmpty()) throw new IllegalStateException("Giỏ hàng đang trống.");
    BigDecimal subtotal = BigDecimal.ZERO, fee = shippingFee(shipping), discount = BigDecimal.ZERO;
    for (CartItem item : items) {
      Product p =
          products
              .findById(item.getProduct().getId())
              .orElseThrow(() -> new IllegalStateException("Sản phẩm không còn tồn tại."));
      p.validateQuantity(item.getQuantity());
      if (p.getStock() == null || p.getStock() < item.getQuantity())
        throw new IllegalStateException("Không đủ tồn kho: " + p.getName());
      subtotal =
          subtotal.add(
              BigDecimal.valueOf(p.getEffectivePrice())
                  .multiply(BigDecimal.valueOf(item.getQuantity())));
    }
    if (code != null && !code.isBlank())
      discount =
          voucherService.discount(
              vouchers
                  .findByCodeIgnoreCase(code.trim())
                  .orElseThrow(() -> new IllegalStateException("Không tìm thấy voucher.")),
              subtotal,
              fee);
    return Map.of(
        "subtotal",
        subtotal,
        "shippingFee",
        fee,
        "discount",
        discount,
        "total",
        subtotal.add(fee).subtract(discount));
  }

  @Transactional
  public void shipmentDetails(
      Long id,
      String carrier,
      String tracking,
      String status,
      java.time.LocalDate estimated,
      String location,
      String note,
      String actor) {
    Order o = orderRepo.lockById(id).orElseThrow();
    if (!Set.of(
            "preparing",
            "handed_over",
            "in_transit",
            "out_for_delivery",
            "delivery_failed",
            "returned")
        .contains(status)) throw new IllegalStateException("Trạng thái vận chuyển không hợp lệ.");
    if (!"preparing".equals(status) && !"SHIPPING".equals(o.getOrderStatus()))
      throw new IllegalStateException(
          "Chuyển đơn sang đang vận chuyển trước khi cập nhật hành trình.");
    if (location != null && location.length() > 150 || note != null && note.length() > 1000)
      throw new IllegalStateException("Thông tin hành trình quá dài.");
    shipment(id, carrier, tracking, note, actor);
    o.setShipmentStatus(status);
    o.setEstimatedDeliveryAt(estimated);
    orderRepo.save(o);
    ShipmentEvent e = new ShipmentEvent();
    e.setOrder(o);
    e.setStatus(status);
    e.setCarrier(carrier);
    e.setTrackingCode(tracking);
    e.setLocation(location);
    e.setNote(note);
    e.setActor(actor);
    shipmentEvents.save(e);
  }

  @Transactional
  public void confirmManualPayment(Long id, boolean received, String actor) {
    if (!received) throw new IllegalStateException("Cần xác nhận đã nhận đủ tiền.");
    Order o = orderRepo.lockById(id).orElseThrow();
    if (!"QR".equals(o.getPaymentMethod()) || "CANCELLED".equals(o.getOrderStatus()))
      throw new IllegalStateException("Chỉ xác nhận chuyển khoản cho đơn QR đang còn hiệu lực.");
    if ("PAID".equals(o.getPaymentStatus())) return;
    if (!orders.markOrderPaid(o.getPaymentCode(), o.getTotalAmount()))
      throw new IllegalStateException("Đơn đã hết thời gian thanh toán.");
    record(o, "Admin xác nhận đã nhận đủ tiền chuyển khoản", actor);
  }
}
