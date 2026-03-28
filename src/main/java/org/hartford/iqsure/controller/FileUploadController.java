// Controller handling FileUploadController related API endpoints
package org.hartford.iqsure.controller;
import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {
    private final FileStorageService fileStorageService;
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String filePath = fileStorageService.storeFile(file);
        return ResponseEntity.ok(Map.of("filePath", filePath));
    }
    @GetMapping("/view/**")
    public ResponseEntity<Resource> getFile(@RequestHeader(value = "Range", required = false) String range) {
        try {
            String path = (String) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                    .getAttribute(org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, 0);
            String fileName = path.substring(path.indexOf("/view/") + 6);
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = "application/pdf";
                if (fileName.toLowerCase().endsWith(".png")) contentType = "image/png";
                if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}