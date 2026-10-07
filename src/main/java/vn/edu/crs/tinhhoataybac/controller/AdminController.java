package vn.edu.crs.tinhhoataybac.controller;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.*;
import vn.edu.crs.tinhhoataybac.service.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
  private final ShipmentEventRepository shipmentEvents;
  private final OrderService orders;
  private final CommerceService commerce;
  private final OrderStatusHistoryRepository histories;
  private final OrderServiceRequestRepository requests;

  public AdminController(
      OrderService orders,
      CommerceService commerce,
      OrderStatusHistoryRepository histories,
      OrderServiceRequestRepository requests,
      ShipmentEventRepository shipmentEvents) {
    this.shipmentEvents = shipmentEvents;
    this.orders = orders;
    this.commerce = commerce;
    this.histories = histories;
    this.requests = requests;
  }

  @GetMapping
  public String home() {
    return "redirect:/admin/dashboard";
  }

  @GetMapping("/orders")
  public String orders(
      @RequestParam(defaultValue = "ALL") String status,
      @RequestParam(defaultValue = "") String keyword,
      Model m) {
    String filter = status.trim().toUpperCase(Locale.ROOT);
    String q = keyword.trim().toLowerCase(Locale.ROOT);
    m.addAttribute(
        "orders",
        orders.getAllOrders().stream()
            .filter(o -> "ALL".equals(filter) || filter.equals(o.getOrderStatus()))
            .filter(
                o ->
                    q.isEmpty()
                        || String.valueOf(o.getId()).contains(q)
                        || contains(o.getCustomerName(), q)
                        || contains(o.getPhone(), q)
                        || contains(o.getEmail(), q)
                        || contains(o.getPaymentCode(), q))
            .toList());
    m.addAttribute("selectedStatus", filter);
    m.addAttribute("keyword", keyword);
    return "admin/orders";
  }

  private boolean contains(String text, String q) {
    return text != null && text.toLowerCase(Locale.ROOT).contains(q);
  }

  @GetMapping("/orders/{id}")
  public String detail(@PathVariable Long id, Model m) {
    Order o = orders.getOrderById(id);
    if (o == null) return "redirect:/admin/orders";
    m.addAttribute("order", o);
    m.addAttribute("histories", histories.findByOrderIdOrderByCreatedAtDesc(id));
    m.addAttribute("requests", requests.findByOrderIdOrderByCreatedAtDesc(id));
    m.addAttribute("shipmentEvents", shipmentEvents.findByOrderIdOrderByCreatedAtDesc(id));
    return "admin/order-detail";
  }

  @PostMapping("/orders/{id}/status")
  public String status(
      @PathVariable Long id, @RequestParam String status, Authentication a, RedirectAttributes f) {
    try {
      commerce.changeStatus(id, status, a.getName());
      f.addFlashAttribute("success", "Đã cập nhật trạng thái.");
    } catch (IllegalStateException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/orders/" + id;
  }

  @PostMapping("/orders/{id}/shipment")
  public String shipment(
      @PathVariable Long id,
      @RequestParam String carrier,
      @RequestParam String trackingCode,
      @RequestParam(required = false) String note,
      @RequestParam(defaultValue = "preparing") String shipmentStatus,
      @RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          java.time.LocalDate estimatedDeliveryAt,
      @RequestParam(required = false) String location,
      Authentication a,
      RedirectAttributes f) {
    try {
      commerce.shipmentDetails(
          id,
          carrier,
          trackingCode,
          shipmentStatus,
          estimatedDeliveryAt,
          location,
          note,
          a.getName());
      f.addFlashAttribute("success", "Đã cập nhật vận chuyển.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/orders/" + id;
  }

  @GetMapping("/service-requests")
  public String requests(Model m) {
    m.addAttribute("requests", requests.findAllByOrderByCreatedAtDesc());
    return "admin/service-requests";
  }

  @PostMapping("/service-requests/{id}")
  public String request(
      @PathVariable Long id,
      @RequestParam String decision,
      @RequestParam(required = false) String note,
      Authentication a,
      RedirectAttributes f) {
    try {
      commerce.processRequest(id, decision, note, a.getName());
      f.addFlashAttribute("success", "Đã xử lý yêu cầu.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/service-requests";
  }

  @PostMapping("/orders/{id}/payment")
  public String confirmPayment(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean received,
      Authentication a,
      RedirectAttributes f) {
    try {
      commerce.confirmManualPayment(id, received, a.getName());
      f.addFlashAttribute("success", "Đã xác nhận thanh toán.");
    } catch (RuntimeException e) {
      f.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/orders/" + id;
  }
}
