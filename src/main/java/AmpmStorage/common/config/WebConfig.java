package AmpmStorage.common.config;

import AmpmStorage.common.interceptor.SignatureVerificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final SignatureVerificationInterceptor signatureVerificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signatureVerificationInterceptor)
            .addPathPatterns("/storage/{fileId}"); // 이 경로에만 인터셉터 적용
    }
}
