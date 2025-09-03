package AmpmStorage.common.interceptor;

import AmpmStorage.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class SignatureVerificationInterceptor implements HandlerInterceptor {

    @Value("${app.storage.secret-key}")
    private String secretKey;
    public static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String expiresStr = request.getParameter("expires");
        String providedSignature = request.getParameter("signature");
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String fileId = pathVariables.get("fileId");

        // 1. 필수 파라미터 확인
        if (expiresStr == null || providedSignature == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "필수 파라미터(expires, signature)가 누락되었습니다.");
        }

        // 2. 만료 시간 확인
        long expiry;
        try {
            expiry = Long.parseLong(expiresStr);
        } catch (NumberFormatException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "만료 시간(expires)이 올바른 숫자 형식이 아닙니다.");
        }

        if (System.currentTimeMillis() / 1000 > expiry) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "요청이 만료되었습니다.");
        }

        // 3. 서명 재생성
        try {
            String methodForSignature = request.getMethod();
            String messageToSign = methodForSignature + "\n" + fileId + "\n" + expiry;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signatureBytes = mac.doFinal(messageToSign.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            // 4. 서명 비교
            if (!providedSignature.equals(expectedSignature)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "서명이 유효하지 않습니다.");
            }
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "서명 처리 중 서버 오류가 발생했습니다.");
        }

        // 모든 검증 통과 시 컨트롤러로 요청 전달
        return true;
    }
}
