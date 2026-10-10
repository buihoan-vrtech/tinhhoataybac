package vn.edu.crs.tinhhoataybac.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import vn.edu.crs.tinhhoataybac.model.Order;
import vn.edu.crs.tinhhoataybac.model.WalletTopUp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;


@Service
public class SePayService {

    @Value("${sepay.merchant-id}")
    private String merchantId;

    @Value("${sepay.secret-key}")
    private String secretKey;

    @Value("${sepay.checkout-url}")
    private String checkoutUrl;

    @Value("${sepay.public-url}")
    private String publicUrl;


    /*
     * =========================================================
     * THANH TOÁN ĐƠN HÀNG
     * =========================================================
     */
    public Map<String, String> buildCheckoutFields(Order order) {

        Map<String, String> fields =
                new LinkedHashMap<>();


        String amount =
                order.getTotalAmount()
                        .stripTrailingZeros()
                        .toPlainString();


        fields.put(
                "order_amount",
                amount
        );


        fields.put(
                "merchant",
                merchantId
        );


        fields.put(
                "currency",
                "VND"
        );


        fields.put(
                "operation",
                "PURCHASE"
        );


        fields.put(
                "order_description",
                "Thanh toan don hang "
                        + order.getPaymentCode()
        );


        fields.put(
                "order_invoice_number",
                order.getPaymentCode()
        );


        fields.put(
                "payment_method",
                "BANK_TRANSFER"
        );


        fields.put(
                "success_url",
                publicUrl
                        + "/payment/sepay/success/"
                        + order.getId()
        );


        fields.put(
                "error_url",
                publicUrl
                        + "/payment/sepay/error/"
                        + order.getId()
        );


        fields.put(
                "cancel_url",
                publicUrl
                        + "/payment/sepay/cancel/"
                        + order.getId()
        );


        fields.put(
                "signature",
                createSignature(fields)
        );


        return fields;
    }


    /*
     * =========================================================
     * NẠP TIỀN VÀO VÍ
     * =========================================================
     */
    public Map<String, String> buildWalletTopUpFields(
            WalletTopUp topUp) {

        Map<String, String> fields =
                new LinkedHashMap<>();


        String amount =
                topUp.getAmount()
                        .stripTrailingZeros()
                        .toPlainString();


        fields.put(
                "order_amount",
                amount
        );


        fields.put(
                "merchant",
                merchantId
        );


        fields.put(
                "currency",
                "VND"
        );


        fields.put(
                "operation",
                "PURCHASE"
        );


        fields.put(
                "order_description",
                "Nap tien Vi Tinh Hoa "
                        + topUp.getPaymentCode()
        );


        fields.put(
                "order_invoice_number",
                topUp.getPaymentCode()
        );


        fields.put(
                "payment_method",
                "BANK_TRANSFER"
        );


        fields.put(
                "success_url",
                publicUrl
                        + "/wallet/topup/success/"
                        + topUp.getId()
        );


        fields.put(
                "error_url",
                publicUrl
                        + "/wallet/topup/error/"
                        + topUp.getId()
        );


        fields.put(
                "cancel_url",
                publicUrl
                        + "/wallet/topup/cancel/"
                        + topUp.getId()
        );


        fields.put(
                "signature",
                createSignature(fields)
        );


        return fields;
    }


    /*
     * =========================================================
     * TẠO CHỮ KÝ SEPAY
     * =========================================================
     */
    private String createSignature(
            Map<String, String> fields) {

        try {

            String[] signedFields = {
                    "order_amount",
                    "merchant",
                    "currency",
                    "operation",
                    "order_description",
                    "order_invoice_number",
                    "customer_id",
                    "payment_method",
                    "success_url",
                    "error_url",
                    "cancel_url"
            };


            StringBuilder signedString =
                    new StringBuilder();


            for (String field : signedFields) {

                if (!fields.containsKey(field)) {
                    continue;
                }


                if (!signedString.isEmpty()) {
                    signedString.append(",");
                }


                signedString
                        .append(field)
                        .append("=")
                        .append(fields.get(field));
            }


            Mac mac =
                    Mac.getInstance(
                            "HmacSHA256"
                    );


            SecretKeySpec keySpec =
                    new SecretKeySpec(
                            secretKey.getBytes(
                                    StandardCharsets.UTF_8
                            ),
                            "HmacSHA256"
                    );


            mac.init(keySpec);


            byte[] hash =
                    mac.doFinal(
                            signedString
                                    .toString()
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );


            return Base64
                    .getEncoder()
                    .encodeToString(hash);


        } catch (Exception e) {

            throw new IllegalStateException(
                    "Không thể tạo chữ ký SePay.",
                    e
            );
        }
    }


    public boolean isSandbox() { return checkoutUrl != null && checkoutUrl.startsWith("https://pay-sandbox.sepay.vn/"); }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }
}