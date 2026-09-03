package com.agribid.nexus.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Local-disk implementation backing the File Upload module. Swappable
 * for an S3/GCS-backed implementation later without touching any
 * service that depends on this class, since callers only ever see
 * the returned relative path string.
 */
@Component
@RequiredArgsConstructor
public class FileStorageUtil {

    @Value("${agribid.storage.base-path}")
    private String basePath;

    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        try {
            Path targetDir = Path.of(basePath, subDirectory);
            Files.createDirectories(targetDir);

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + (extension != null ? "." + extension : "");
            Path targetFile = targetDir.resolve(fileName);

            file.transferTo(targetFile);

            return subDirectory + "/" + fileName;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    public byte[] loadBytes(String imageUrl) {
        try {
            Path filePath = Path.of(basePath, imageUrl);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read stored image: " + imageUrl, e
            );
        }
    }
}
