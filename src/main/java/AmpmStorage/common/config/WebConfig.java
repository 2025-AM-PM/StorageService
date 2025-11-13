package AmpmStorage.common.config;

import AmpmStorage.common.interceptor.SignatureVerificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final SignatureVerificationInterceptor signatureVerificationInterceptor;
    private final String frontendDomain;

    public WebConfig(SignatureVerificationInterceptor signatureVerificationInterceptor,
                     @Value("${frontend.domain}") String frontendDomain) {
        this.signatureVerificationInterceptor = signatureVerificationInterceptor;
        this.frontendDomain = frontendDomain;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signatureVerificationInterceptor)
            .addPathPatterns("/storage/{fileId}"); // 이 경로에만 인터셉터 적용
    }

    // Todo: CORS 설정 다시 확인
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 허용
                .allowedOrigins("null", frontendDomain)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
