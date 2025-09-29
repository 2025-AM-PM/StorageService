package AmpmStorage.common.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SignatureValidatorTest {

    private SignatureValidator signatureValidator;
    private final String TEST_SECRET_KEY = "test-secret-key-for-junit";

    @BeforeEach
    void setUp() {
        // 테스트용 Validator 인스턴스 직접 생성
        signatureValidator = new SignatureValidator(TEST_SECRET_KEY);
    }

    @Test
    @DisplayName("유효한 서명과 만료되지 않은 시간으로 검증에 성공한다")
    void isValid_withValidSignatureAndNotExpired_shouldReturnTrue() {
        // given
        String httpMethod = "GET";
        String fileId = "test-file-id";
        long expires = System.currentTimeMillis() / 1000 + 3600; // 1시간 후 만료
        String signature = signatureValidator.generateSignature(httpMethod, fileId, expires);

        // when
        boolean result = signatureValidator.isValid(httpMethod, fileId, expires, signature);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("서명이 틀렸을 경우 검증에 실패한다")
    void isValid_withInvalidSignature_shouldReturnFalse() {
        // given
        String httpMethod = "GET";
        String fileId = "test-file-id";
        long expires = System.currentTimeMillis() / 1000 + 3600;
        String invalidSignature = "this-is-a-wrong-signature";

        // when
        boolean result = signatureValidator.isValid(httpMethod, fileId, expires, invalidSignature);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("URL이 만료되었을 경우 검증에 실패한다")
    void isValid_whenUrlIsExpired_shouldReturnFalse() {
        // given
        String httpMethod = "GET";
        String fileId = "test-file-id";
        long expires = System.currentTimeMillis() / 1000 - 60; // 1분 전 만료
        String signature = signatureValidator.generateSignature(httpMethod, fileId, expires);

        // when
        boolean result = signatureValidator.isValid(httpMethod, fileId, expires, signature);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("서명된 메시지 내용(fileId)이 다를 경우 검증에 실패한다")
    void isValid_withDifferentFileId_shouldReturnFalse() {
        // given
        String httpMethod = "GET";
        String originalFileId = "original-file-id";
        String differentFileId = "different-file-id";
        long expires = System.currentTimeMillis() / 1000 + 3600;

        // 서명은 originalFileId로 생성
        String signature = signatureValidator.generateSignature(httpMethod, originalFileId, expires);

        // 검증은 differentFileId로 시도
        // when
        boolean result = signatureValidator.isValid(httpMethod, differentFileId, expires, signature);

        // then
        assertThat(result).isFalse();
    }
}