package AmpmStorage.common.validator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class SignatureValidator {

    private final String secretKey;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public SignatureValidator(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isValid(String httpMethod, String fileId, long expires, String providedSignature) {
        if (System.currentTimeMillis() / 1000 > expires) {
            return false;
        }
        String expectedSignature = generateSignature(httpMethod, fileId, expires);
        return Objects.equals(providedSignature, expectedSignature);
    }

    public String generateSignature(String httpMethod, String fileId, long expires) {
        String messageToSign = httpMethod + "\n" + fileId + "\n" + expires;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signatureBytes = mac.doFinal(messageToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("서명 생성에 실패했습니다.", e);
        }
    }
}