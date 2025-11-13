package AmpmStorage.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod; // HttpMethod 임포트
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. [필수] WebConfig.java의 CORS 설정을 Security 필터체인에 적용
                .cors(withDefaults())

                // 2. CSRF는 비활성화 (필요에 따라)
                .csrf(csrf -> csrf.disable())

                // 3. 요청 경로별 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        // 4. [필수] 모든 'OPTIONS' 예비 요청은 인증 없이 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 5. (예시) 파일 업로드(PUT) 및 다운로드(GET) 경로는 인증 필요
                        // .requestMatchers(HttpMethod.PUT, "/storage/exhibits/**").authenticated()
                        // .requestMatchers(HttpMethod.GET, "/storage/exhibits/**").authenticated()

                        // 6. 나머지 모든 요청 허용 (또는 인증 필요)
                        .anyRequest().permitAll() // 또는 .authenticated()
                );

        return http.build();
    }
}