package vn.edu.crs.tinhhoataybac.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.crs.tinhhoataybac.dto.SePayWebhookRequest;
import vn.edu.crs.tinhhoataybac.model.SePayTransaction;
import vn.edu.crs.tinhhoataybac.repository.SePayTransactionRepository;
import vn.edu.crs.tinhhoataybac.service.OrderService;

import java.util.Map;

@RestController
@RequestMapping("/api/sepay")
public class SePayWebhookController {

    @org.springframework.beans.factory.annotation.Value("${sepay.webhook-secret:}")
    private String webhookSecret;

    @org.springframework.beans.factory.annotation.Value("${payment.qr.account-no:}")
    private String receivingAccount;

    private final OrderService orderService;

    private final SePayTransactionRepository
            sePayTransactionRepository;


    public SePayWebhookController(
            OrderService orderService,
            SePayTransactionRepository sePayTransactionRepository) {

        this.orderService = orderService;

        this.sePayTransactionRepository =
                sePayTransactionRepository;
    }


    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> webhook(
            @RequestHeader(value="Authorization", required=false) String authorization,
            @RequestBody SePayWebhookRequest request) {

        String expected = "Apikey " + webhookSecret;
        if (webhookSecret == null || webhookSecret.isBlank() || authorization == null || !java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8), authorization.getBytes(java.nio.charset.StandardCharsets.UTF_8))) return ResponseEntity.status(401).body(Map.of("success", false));
        if (receivingAccount == null || receivingAccount.isBlank() || !receivingAccount.equals(request.getAccountNumber())) return ResponseEntity.badRequest().body(Map.of("success", false));
        if (request.getTransferAmount() == null || request.getTransferAmount().signum() <= 0 || request.getId() == null || request.getId() <= 0) return ResponseEntity.badRequest().body(Map.of("success", false));
        try {

            /*
             * Chỉ nhận giao dịch tiền vào.
             */
            if (!"in".equalsIgnoreCase(
                    request.getTransferType()
            )) {

                return ResponseEntity.ok(
                        Map.of("success", true)
                );
            }


            /*
             * Không có transaction id.
             */
            if (request.getId() == null) {

                return ResponseEntity.ok(
                        Map.of("success", true)
                );
            }


            /*
             * Chống xử lý giao dịch 2 lần.
             */
            if (sePayTransactionRepository
                    .existsBySepayTransactionId(
                            request.getId()
                    )) {

                return ResponseEntity.ok(
                        Map.of("success", true)
                );
            }


            /*
             * Phải có mã thanh toán.
             *
             * Ví dụ:
             *
             * THB15
             */
            String paymentCode = request.getCode();
            if (paymentCode == null || paymentCode.isBlank()) {
                var match=java.util.regex.Pattern.compile("(?i)(?<![A-Z0-9])THB[1-9][0-9]{0,18}(?![A-Z0-9])").matcher(request.getContent() == null ? "" : request.getContent());
                if (match.find()) paymentCode=match.group();
            }
            if (paymentCode != null) paymentCode=paymentCode.trim();


            if (paymentCode == null
                    || paymentCode.isBlank()) {

                return ResponseEntity.ok(
                        Map.of("success", true)
                );
            }


            /*
             * Chỉ nhận mã của website.
             */
            if (!paymentCode
                    .toUpperCase()
                    .matches("THB[1-9][0-9]{0,18}")) {

                return ResponseEntity.ok(
                        Map.of("success", true)
                );
            }


            /*
             * Xác nhận đơn.
             */
            boolean paid =
                    orderService.markOrderPaid(
                            paymentCode.toUpperCase(),
                            request.getTransferAmount()
                    );


            /*
             * Chỉ lưu giao dịch nếu đã
             * tìm thấy đơn phù hợp.
             */
            if (paid) {

                SePayTransaction transaction =
                        new SePayTransaction();


                transaction.setSepayTransactionId(
                        request.getId()
                );

                transaction.setGateway(
                        request.getGateway()
                );

                transaction.setTransactionDate(
                        request.getTransactionDate()
                );

                transaction.setAccountNumber(
                        request.getAccountNumber()
                );

                transaction.setPaymentCode(
                        paymentCode
                );

                transaction.setContent(
                        request.getContent()
                );

                transaction.setTransferType(
                        request.getTransferType()
                );

                transaction.setTransferAmount(
                        request.getTransferAmount()
                );

                transaction.setReferenceCode(
                        request.getReferenceCode()
                );


                sePayTransactionRepository.save(
                        transaction
                );
            }


            /*
             * SePay yêu cầu:
             *
             * HTTP 200
             * {"success": true}
             */
            return ResponseEntity.ok(
                    Map.of("success", true)
            );


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of("success", false)
                    );
        }
    }
}