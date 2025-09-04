package AmpmStorage.storage.controller;

import AmpmStorage.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
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

    public StorageController(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            // 애플리케이션 시작 시 디렉터리 생성 실패는 심각한 오류이므로 RuntimeException 유지
            throw new RuntimeException("파일을 저장할 디렉터리를 생성할 수 없습니다.", ex);
        }
    }

    @PutMapping(value = "/{fileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@PathVariable String fileId,
                                             @Parameter(schema = @Schema(type = "string", format = "binary")) @RequestParam("file") MultipartFile file) {
        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileId);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved to: " + targetLocation.toAbsolutePath());
            return ResponseEntity.ok("File uploaded successfully: " + fileId);
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileId).normalize();
            System.out.println("Attempting to load file from: " + filePath.toAbsolutePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            } else {
                throw new BusinessException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없거나 읽을 수 없습니다: " + fileId);
            }
        } catch (MalformedURLException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "파일 경로가 올바르지 않습니다: " + fileId);
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "파일에 접근하는 중 오류가 발생했습니다.");
        }
    }
}
