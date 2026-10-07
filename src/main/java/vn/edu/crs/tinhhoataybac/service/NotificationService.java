package vn.edu.crs.tinhhoataybac.service;

import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.model.*;
import vn.edu.crs.tinhhoataybac.repository.CustomerNotificationRepository;

@Service
public class NotificationService {
  private final CustomerNotificationRepository notifications;

  public NotificationService(CustomerNotificationRepository notifications) {
    this.notifications = notifications;
  }

  public void notify(User user, Long orderId, String message) {
    if (user == null) return;
    var n = new CustomerNotification();
    n.setUser(user);
    n.setOrderId(orderId);
    n.setMessage(message);
    notifications.save(n);
  }
}
