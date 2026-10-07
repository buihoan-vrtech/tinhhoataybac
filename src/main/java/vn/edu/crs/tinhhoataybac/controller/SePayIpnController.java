package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.crs.tinhhoataybac.service.OrderService;
import vn.edu.crs.tinhhoataybac.service.WalletTopUpService;

import java.math.BigDecimal;
import java.util.Map;


@RestController
@RequestMapping("/api/sepay")
public class SePayIpnController {

    private final OrderService orderService;

    private final WalletTopUpService walletTopUpService;


    public SePayIpnController(
            OrderService orderService,
            WalletTopUpService walletTopUpService) {

        this.orderService = orderService;

        this.walletTopUpService =
                walletTopUpService;
    }


    @PostMapping("/ipn")
    public ResponseEntity<?> receiveIpn(
            @RequestBody Map<String, Object> payload) {

        try {

            System.out.println(
                    "========== SEPAY IPN =========="
            );

            System.out.println(payload);

            System.out.println(
                    "================================"
            );


            /*
             * =========================================
             * KIỂM TRA LOẠI THÔNG BÁO
             * =========================================
             */
            Object notificationType =
                    payload.get("notification_type");


            System.out.println(
                    "notification_type = "
                            + notificationType
            );


            if (!"ORDER_PAID".equals(
                    String.valueOf(notificationType)
            )) {

                return ResponseEntity.ok(
                        Map.of(
                                "success",
                                true
                        )
                );
            }


            /*
             * =========================================
             * LẤY OBJECT ORDER TỪ PAYLOAD
             * =========================================
             */
            Object orderObject =
                    payload.get("order");


            if (!(orderObject instanceof Map<?, ?>)) {

                System.out.println(
                        "Không tìm thấy order trong IPN"
                );


                return ResponseEntity.ok(
                        Map.of(
                                "success",
                                true
                        )
                );
            }


            @SuppressWarnings("unchecked")
            Map<String, Object> orderData =
                    (Map<String, Object>) orderObject;


            /*
             * =========================================
             * LẤY MÃ THANH TOÁN
             * =========================================
             */
            Object invoiceObject =
                    orderData.get(
                            "order_invoice_number"
                    );


            if (invoiceObject == null) {

                System.out.println(
                        "Không có order_invoice_number"
                );


                return ResponseEntity.ok(
                        Map.of(
                                "success",
                                true
                        )
                );
            }


            String paymentCode =
                    String.valueOf(
                                    invoiceObject
                            )
                            .trim()
                            .toUpperCase();


            /*
             * =========================================
             * LẤY SỐ TIỀN
             * =========================================
             */
            Object amountObject =
                    orderData.get(
                            "order_amount"
                    );


            if (amountObject == null) {

                System.out.println(
                        "Không có order_amount"
                );


                return ResponseEntity.ok(
                        Map.of(
                                "success",
                                true
                        )
                );
            }


            BigDecimal amount =
                    new BigDecimal(
                            String.valueOf(
                                    amountObject
                            )
                    );


            System.out.println(
                    "paymentCode = "
                            + paymentCode
            );


            System.out.println(
                    "amount = "
                            + amount
            );


            /*
             * =========================================
             * THANH TOÁN ĐƠN HÀNG
             *
             * THB1
             * THB2
             * THB3
             * =========================================
             */
            if (paymentCode.startsWith("THB")) {

                boolean result =
                        orderService.markOrderPaid(
                                paymentCode,
                                amount
                        );


                System.out.println(
                        "ORDER markOrderPaid = "
                                + result
                );


                return ResponseEntity.ok(
                        Map.of(
                                "success",
                                true
                        )
                );
            }


            /*
             * =========================================
             * NẠP TIỀN VÀO VÍ
             *
             * NAP1
             * NAP2
             * NAP3
             * =========================================
             */
            if (paymentCode.startsWith("NAP")) {

                boolean result =
                        walletTopUpService.confirmTopUp(
                                paymentCode,
                                amount
                        );


                System.out.println(
                        "WALLET confirmTopUp = "
                                + result
                );


                return ResponseEntity.ok(
                        Map.of(
                                "success",
                                true
                        )
                );
            }


            /*
             * =========================================
             * MÃ KHÔNG XÁC ĐỊNH
             * =========================================
             */
            System.out.println(
                    "Không nhận diện được paymentCode: "
                            + paymentCode
            );


            return ResponseEntity.ok(
                    Map.of(
                            "success",
                            true
                    )
            );


        } catch (Exception e) {

            e.printStackTrace();


            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "success",
                                    false
                            )
                    );
        }
    }
}