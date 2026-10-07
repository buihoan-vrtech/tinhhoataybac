package vn.edu.crs.tinhhoataybac.service;

import java.math.*;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import vn.edu.crs.tinhhoataybac.model.Voucher;

@Service
public class VoucherService {
  public BigDecimal discount(Voucher v, BigDecimal subtotal, BigDecimal shipping) {
    var now = LocalDateTime.now();
    if (v == null
        || !Boolean.TRUE.equals(v.getActive())
        || (v.getStartsAt() != null && now.isBefore(v.getStartsAt()))
        || (v.getExpiresAt() != null && !now.isBefore(v.getExpiresAt()))
        || (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()))
      throw new IllegalStateException("Voucher không còn hiệu lực.");
    if (subtotal.compareTo(v.getMinOrderValue()) < 0)
      throw new IllegalStateException("Đơn hàng chưa đạt giá trị tối thiểu của voucher.");
    BigDecimal amount =
        switch (v.getType()) {
          case "percent" ->
              subtotal
                  .multiply(v.getValue().min(new BigDecimal("100")))
                  .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
          case "fixed" -> v.getValue().min(subtotal);
          case "shipping" -> v.getValue().min(shipping);
          default -> throw new IllegalStateException("Loại voucher không hợp lệ.");
        };
    if (v.getMaxDiscount() != null) amount = amount.min(v.getMaxDiscount());
    return amount.max(BigDecimal.ZERO).min(subtotal.add(shipping));
  }
}
