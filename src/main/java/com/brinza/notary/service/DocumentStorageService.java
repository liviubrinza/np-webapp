package com.brinza.notary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Stores uploaded documents on the filesystem under {@code app.storage.documents-dir}, nested by
 * upload date / client / service, e.g. {@code <root>/2026/05/May/Joe_Mack/Document_Authentication/file.pdf}.
 */
@Component
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final Path documentsRoot;

    public DocumentStorageService(@Value("${app.storage.documents-dir}") String documentsDir) {
        this.documentsRoot = Path.of(documentsDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String clientName, String serviceName) {
        log.info("store called for filename={} clientName={} serviceName={}", file.getOriginalFilename(), clientName, serviceName);
        LocalDate today = LocalDate.now();
        Path targetDir = documentsRoot
                .resolve(String.valueOf(today.getYear()))
                .resolve("%02d".formatted(today.getMonthValue()))
                .resolve(today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .resolve(sanitize(clientName))
                .resolve(sanitize(serviceName));

        try {
            Files.createDirectories(targetDir);
            String filename = uniqueFilename(targetDir, sanitizeFilename(file.getOriginalFilename()));
            Path targetFile = targetDir.resolve(filename);
            file.transferTo(targetFile);
            String relativePath = documentsRoot.relativize(targetFile).toString().replace('\\', '/');
            log.debug("Stored file at relativePath={}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.debug("Failed to store uploaded file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new UncheckedIOException("Failed to store uploaded file " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Deletes the file and then removes its parent directory, and any now-empty ancestor
     * directories up to (not including) {@code documentsRoot}, so the date/client/service
     * folders created for a document don't linger once it's the last file removed from them.
     */
    public void delete(String relativePath) {
        log.info("delete called for relativePath={}", relativePath);
        Path file = documentsRoot.resolve(relativePath).normalize();
        if (!file.startsWith(documentsRoot)) {
            log.debug("Rejected delete: resolved path escapes storage root for relativePath={}", relativePath);
            throw new SecurityException("Resolved document path escapes the storage root: " + relativePath);
        }
        try {
            boolean deleted = Files.deleteIfExists(file);
            log.debug("File deletion for {} succeeded={}", file, deleted);
            removeEmptyAncestors(file.getParent());
        } catch (IOException e) {
            log.debug("Failed to delete document file {}: {}", relativePath, e.getMessage());
            throw new UncheckedIOException("Failed to delete document file " + relativePath, e);
        }
    }

    private void removeEmptyAncestors(Path dir) throws IOException {
        log.debug("removeEmptyAncestors called starting at dir={}", dir);
        while (dir != null && dir.startsWith(documentsRoot) && !dir.equals(documentsRoot)) {
            try (var listing = Files.list(dir)) {
                if (listing.findAny().isPresent()) {
                    break;
                }
            }
            Files.delete(dir);
            dir = dir.getParent();
        }
    }

    public Resource loadAsResource(String relativePath) {
        log.info("loadAsResource called for relativePath={}", relativePath);
        Path file = documentsRoot.resolve(relativePath).normalize();
        if (!file.startsWith(documentsRoot)) {
            log.debug("Rejected loadAsResource: resolved path escapes storage root for relativePath={}", relativePath);
            throw new SecurityException("Resolved document path escapes the storage root: " + relativePath);
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.debug("Document file not found or unreadable on disk: {}", file);
                throw new NoSuchElementException("Document file not found on disk: " + relativePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String uniqueFilename(Path dir, String filename) {
        if (Files.notExists(dir.resolve(filename))) {
            return filename;
        }
        String base = filename;
        String extension = "";
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            base = filename.substring(0, dot);
            extension = filename.substring(dot);
        }
        int counter = 1;
        String candidate;
        do {
            candidate = base + "(" + counter + ")" + extension;
            counter++;
        } while (Files.exists(dir.resolve(candidate)));
        return candidate;
    }

    private static String sanitizeFilename(String originalFilename) {
        String name = originalFilename == null ? "document" : Path.of(originalFilename).getFileName().toString();
        return sanitize(name);
    }

    private static String sanitize(String value) {
        String cleaned = value.trim().replaceAll("[<>:\"/\\\\|?*]", "").replaceAll("\\s+", "_");
        return cleaned.isBlank() ? "unnamed" : cleaned;
    }
}
