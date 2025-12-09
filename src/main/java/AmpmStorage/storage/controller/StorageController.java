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



    // 1. PutMapping 경로를 Presigned URL 생성 경로와 동일하게 수정합니다.
    @PutMapping(
            path = "/exhibits/images/{userId}/posts/{uuid}/image.png",
            consumes = "image/*"
    )
    public ResponseEntity<String> handleRawFileUpload(
            @PathVariable String userId,
            @PathVariable String uuid,
            // @PathVariable String fileName, // "image.png"은 경로에 고정되었으므로 삭제
            HttpServletRequest request) {

        try {
            // 2. 경로 변수를 기반으로 실제 저장 경로를 조합합니다.
            // "fileName"이 "image.png"로 고정되었습니다.
            String relativePath = String.format("exhibits/images/%s/posts/%s/image.png", userId, uuid);

            // 3. 파일을 저장할 절대 경로를 계산합니다.
            Path destinationFile = Paths.get(this.uploadDir).resolve(relativePath).normalize();

            // 4. 상위 디렉터리가 없으면 생성합니다.
            Files.createDirectories(destinationFile.getParent());

            // 5. 요청 본문에서 파일 저장
            Files.copy(request.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Raw file saved to: " + destinationFile.toAbsolutePath());
            return ResponseEntity.ok("File uploaded successfully: " + relativePath);

        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }
    }
    @DeleteMapping(path = "/exhibits/images/{userId}/posts/{uuid}/image.png")
    public ResponseEntity<String> handleRawFileDelete(
            @PathVariable String userId,
            @PathVariable String uuid) {

        try {
            // 1. 경로 변수를 기반으로 삭제할 파일의 상대 경로를 조합합니다.
            // (handleRawFileUpload 메서드와 동일한 로직)
            String relativePath = String.format("exhibits/images/%s/posts/%s/image.png", userId, uuid);

            // 2. 파일을 삭제할 절대 경로를 계산합니다.
            Path destinationFile = Paths.get(this.uploadDir).resolve(relativePath).normalize();

            // 3. Files.deleteIfExists()를 사용하여 파일이 존재하면 삭제합니다.
            //    (파일이 없어도 오류가 발생하지 않습니다 - 멱등성)
            boolean deleted = Files.deleteIfExists(destinationFile);

            if (deleted) {
                System.out.println("File deleted successfully: " + destinationFile.toAbsolutePath());

                // (선택 사항) 비어있는 상위 디렉터리를 삭제하는 로직을 추가할 수 있습니다.
                // try {
                //     Path parentDir = destinationFile.getParent(); // .../{uuid}
                //     Files.deleteIfExists(parentDir);
                // } catch (IOException e) {
                //     // 디렉터리가 비어있지 않으면(삭제 실패) 무시합니다.
                // }

            } else {
                System.out.println("File to delete not found (already deleted): " + destinationFile.toAbsolutePath());
            }

            // 파일이 성공적으로 삭제되었거나 이미 존재하지 않으므로 200 OK 반환
            return ResponseEntity.ok("File deleted or already absent: " + relativePath);

        } catch (IOException e) {
            // 디렉터리 권한 문제 등 예기치 못한 I/O 오류
            e.printStackTrace();
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다.");
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

    @DeleteMapping("/exhibits/images/{userId}/{uuid}/{fileName}")
    public ResponseEntity<Void> handleRawFileDelete(
            @PathVariable String userId,
            @PathVariable String uuid,
            @PathVariable String fileName) {

        try {
            // 1. GET 메서드와 동일하게 파일 경로를 조합합니다.
            String relativePath = String.format("exhibits/images/%s/posts/%s/%s", userId, uuid, fileName);

            // 2. GET 메서드와 동일하게 절대 경로를 계산합니다.
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();

            // 3. 파일이 존재하면 삭제합니다.
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                // 4. 성공적으로 삭제된 경우 (GET의 'resource.exists()'가 true인 경우와 유사)
                System.out.println("File deleted: " + filePath.toAbsolutePath());
                return ResponseEntity.ok().build(); // 200 OK (또는 204 No Content)
            } else {
                // 5. 파일이 존재하지 않는 경우 (GET의 'else' 블록과 유사)
                System.out.println("File not found (already deleted): " + filePath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException ex) {
            // 6. 권한 문제 등 I/O 오류 (GET의 'catch' 블록과 유사)
            ex.printStackTrace(); // 디버깅을 위해 에러 로그 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
