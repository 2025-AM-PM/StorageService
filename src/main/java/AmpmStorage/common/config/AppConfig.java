package AmpmStorage.common.config;

import AmpmStorage.common.validator.SignatureValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public SignatureValidator signatureValidator(@Value("${app.storage.secret-key}") String secretKey) {
        return new SignatureValidator(secretKey);
    }
}