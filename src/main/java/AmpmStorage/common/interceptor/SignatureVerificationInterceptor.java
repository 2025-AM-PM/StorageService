package AmpmStorage.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class SignatureVerificationInterceptor implements HandlerInterceptor {

    @Value("${app.storage.secret-key}")
    private String secretKey;
    public static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String expiresStr = request.getParameter("expires");
        String providedSignature = request.getParameter("signature");
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String fileId = pathVariables.get("fileId");

        // 1. 필수 파라미터 확인
        if (expiresStr == null || providedSignature == null || fileId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
            return false;
        }

        // 2. 만료 시간 확인
        long expiry = Long.parseLong(expiresStr);
        if (System.currentTimeMillis() / 1000 > expiry) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "URL has expired");
            return false;
        }

        // 3. 서명 재생성 (백엔드와 반드시 동일한 로직)
        String messageToSign = request.getMethod() + "\n" + fileId + "\n" + expiry;
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] signatureBytes = mac.doFinal(messageToSign.getBytes(StandardCharsets.UTF_8));
        String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        // 4. 서명 비교
        if (!providedSignature.equals(expectedSignature)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid signature");
            return false;
        }

        // 모든 검증 통과 시 컨트롤러로 요청 전달
        return true;
    }
}
