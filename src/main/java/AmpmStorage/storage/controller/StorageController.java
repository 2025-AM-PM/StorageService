package AmpmStorage.storage.controller;

import AmpmStorage.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final Path fileStorageLocation;
    private final String uploadDir; // 업로드 디렉토리 경로를 저장할 변수

    public StorageController(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir; // 경로 저장
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일을 저장할 디렉터리를 생성할 수 없습니다.", ex);
        }
    }

    // ⭐️ [신규 추가] Presigned URL을 위한 원시 데이터(raw) PUT 업로드 메서드
    @PutMapping("/exhibits/images/{userId}/{uuid}/{fileName}")
    public ResponseEntity<String> handleRawFileUpload(
        @PathVariable String userId,
        @PathVariable String uuid,
        @PathVariable String fileName,
        HttpServletRequest request) { // MultipartFile 대신 HttpServletRequest를 사용

        try {
            // 1. Presigned URL의 경로 구조에 맞춰 전체 파일 경로를 조합합니다.
            String relativePath = String.format("exhibits/images/%s/%s/%s", userId, uuid, fileName);

            // 2. 파일을 저장할 절대 경로를 계산합니다.
            Path destinationFile = Paths.get(this.uploadDir).resolve(relativePath).normalize();

            // 3. 상위 디렉터리가 없으면 생성합니다.
            Files.createDirectories(destinationFile.getParent());

            // 4. 요청의 본문(body)으로부터 파일 데이터를 직접 읽어와 저장합니다.
            Files.copy(request.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Raw file saved to: " + destinationFile.toAbsolutePath());
            return ResponseEntity.ok("File uploaded successfully: " + relativePath);

        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }
    }

    // 기존의 multipart/form-data 방식 업로드 메서드 (참고용으로 유지)
    @PutMapping(value = "/{fileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@PathVariable String fileId,
        @Parameter(schema = @Schema(type = "string", format = "binary")) @RequestParam("file") MultipartFile file) {
        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileId);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Multipart file saved to: " + targetLocation.toAbsolutePath());
            return ResponseEntity.ok("File uploaded successfully: " + fileId);
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하는 중 오류가 발생했습니다.");
        }
    }

//    @GetMapping("/{fileId}")
//    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
//        try {
//            Path filePath = this.fileStorageLocation.resolve(fileId).normalize();
//            System.out.println("Attempting to load file from: " + filePath.toAbsolutePath());
//            Resource resource = new UrlResource(filePath.toUri());
//
//            if (resource.exists() && resource.isReadable()) {
//                return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
//                    .body(resource);
//            } else {
//                throw new BusinessException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없거나 읽을 수 없습니다: " + fileId);
//            }
//        } catch (MalformedURLException ex) {
//            throw new BusinessException(HttpStatus.BAD_REQUEST, "파일 경로가 올바르지 않습니다: " + fileId);
//        } catch (Exception ex) {
//            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일에 접근하는 중 오류가 발생했습니다.");
//        }
//    }

//    @GetMapping("/{fileId}")
//    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
//        try {
//            Path filePath = this.fileStorageLocation.resolve(fileId).normalize();
//            Resource resource = new UrlResource(filePath.toUri());
//
//            if (resource.exists() && resource.isReadable()) {
//                String contentType = null;
//
//                // [핵심 수정 1] 경로에서 파일 이름을 문자열로 가져옵니다.
//                String filename = filePath.getFileName().toString();
//
//                // [핵심 수정 2] 파일 이름이 "img"와 일치하는지 확인합니다.
//                if ("img".equals(filename)) {
//                    // 이름이 "img"이면, 기본 이미지 타입으로 지정합니다.
//                    // 대부분의 경우 JPEG 또는 PNG이므로 "image/jpeg"를 기본값으로 사용합니다.
//                    // 만약 모든 이미지가 PNG라면 "image/png"로 변경해도 좋습니다.
//                    contentType = "image/jpeg";
//                } else {
//                    // 다른 파일 이름의 경우, 기존 방식대로 확장자를 통해 타입을 추측합니다.
//                    contentType = Files.probeContentType(filePath);
//                }
//
//                // 만약 위 로직으로도 타입을 알 수 없다면, 최종 기본값으로 설정합니다.
//                if (contentType == null) {
//                    contentType = "application/octet-stream";
//                }
//
//                return ResponseEntity.ok()
//                    .contentType(MediaType.parseMediaType(contentType))
//                    .body(resource);
//            } else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//            }
//        } catch (IOException ex) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

    @GetMapping("/exhibits/images/{userId}/{uuid}/{fileName}")
    public ResponseEntity<Resource> handleRawFileDownload(
        @PathVariable String userId,
        @PathVariable String uuid,
        @PathVariable String fileName) {

        try {
            // 1. Presigned URL의 경로 구조에 맞춰 전체 파일 경로를 조합합니다.
            String relativePath = String.format("exhibits/images/%s/%s/%s", userId, uuid, fileName);

            // 2. 파일을 저장할 절대 경로를 계산합니다.
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = null;
                String retrievedFilename = filePath.getFileName().toString();

                // 파일 이름이 "img"인 경우를 특별 처리하는 로직
                if ("img".equals(retrievedFilename)) {
                    contentType = "image/jpeg";
                } else {
                    // 그 외의 경우, 파일 확장자를 통해 Content-Type을 추측
                    contentType = Files.probeContentType(filePath);
                }

                // Content-Type을 결정할 수 없는 경우를 위한 기본값
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException ex) {
            ex.printStackTrace(); // 디버깅을 위해 에러 로그 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
