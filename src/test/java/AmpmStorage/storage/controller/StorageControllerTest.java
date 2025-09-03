package AmpmStorage.storage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.storage.secret-key}")
    private String secretKey;

    @TempDir
    static Path tempDir;

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
        String httpMethod = "POST"; // 서명 생성은 POST로 고정
        long expires = System.currentTimeMillis() / 1000 + 300; // 5분 후 만료

        // 인터셉터와 동일한 로직으로 서명 생성
        String messageToSign = httpMethod + "\n" + fileId + "\n" + expires;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signatureBytes = mac.doFinal(messageToSign.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "original-filename.txt",
            "text/plain",
            fileContent.getBytes(StandardCharsets.UTF_8)
        );

        // when (실행)
        // MockMvc의 multipart는 PUT을 직접 지원하지 않으므로, POST로 보낸 뒤 메소드를 PUT으로 변경
        mockMvc.perform(multipart("/storage/{fileId}", fileId)
                .file(mockFile)
                .param("expires", String.valueOf(expires))
                .param("signature", signature)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
            .andExpect(status().isOk());

        // then (검증)
        Path expectedFilePath = tempDir.resolve(fileId);
        assertThat(Files.exists(expectedFilePath)).isTrue();
        assertThat(Files.readString(expectedFilePath)).isEqualTo(fileContent);
    }
}
