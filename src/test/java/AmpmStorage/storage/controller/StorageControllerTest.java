package AmpmStorage.storage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import AmpmStorage.common.validator.SignatureValidator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StorageControllerTest {

    // 테스트 실행 시, @Primary가 붙은 아래의 Bean이 우선적으로 사용됩니다.
    @TestConfiguration
    static class TestConfig {
        @Primary
        @Bean
        public SignatureValidator signatureValidator(@Value("${app.storage.secret-key}") String secretKey) {
            return new SignatureValidator(secretKey);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SignatureValidator signatureValidator;

    @TempDir
    static Path tempDir;

    // 테스트 실행 동안 'app.storage.upload-dir' 값을 임시 디렉터리 경로로 설정합니다.
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.upload-dir", () -> tempDir.toString());
    }

    @Test
    @DisplayName("유효한 서명으로 파일 업로드 요청 시, 파일이 임시 디렉터리에 성공적으로 생성된다")
    void uploadFile_withValidSignature_shouldCreateFileInTempDir() throws Exception {
        // given (준비)
        String fileId = "test-upload-file-123";
        String fileContent = "Hello, World!";
        String httpMethod = "PUT";
        long expires = System.currentTimeMillis() / 1000 + 300; // 5분 후 만료
        String signature = signatureValidator.generateSignature(httpMethod, fileId, expires);

        String signedUrl = String.format("/storage/%s?expires=%d&signature=%s",
            fileId,
            expires,
            URLEncoder.encode(signature, StandardCharsets.UTF_8)
        );

        // when (실행)
        mockMvc.perform(put(signedUrl) // 1. multipart() -> put()으로 변경
                .contentType(MediaType.TEXT_PLAIN) // 2. Content-Type 설정
                .content(fileContent.getBytes(StandardCharsets.UTF_8))) // 3. .file() 대신 .content() 사용
            .andExpect(status().isOk()); // then (검증) - 1. HTTP 응답 상태 검증

        // then (검증) - 2. 실제 파일이 생성되었는지 파일 시스템에서 직접 확인
        Path expectedFilePath = tempDir.resolve(fileId);
        assertThat(Files.exists(expectedFilePath)).isTrue();
        assertThat(Files.readString(expectedFilePath)).isEqualTo(fileContent);
    }
}
