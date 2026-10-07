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
            @RequestBody SePayWebhookRequest request) {

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
            String paymentCode =
                    request.getCode();


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
                    .startsWith("THB")) {

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