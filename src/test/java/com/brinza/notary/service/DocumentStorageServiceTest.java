package com.brinza.notary.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private DocumentStorageService service() {
        return new DocumentStorageService(tempDir.toString());
    }

    @Test
    void storeCreatesNestedDateClientServiceDirectories() throws Exception {
        MultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "content".getBytes());

        String relativePath = service().store(file, "Ion Popescu", "Document Authentication");

        LocalDate today = LocalDate.now();
        Path expectedDir = tempDir
                .resolve(String.valueOf(today.getYear()))
                .resolve("%02d".formatted(today.getMonthValue()))
                .resolve(today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .resolve("Ion_Popescu")
                .resolve("Document_Authentication");
        assertThat(Files.isDirectory(expectedDir)).isTrue();
        assertThat(relativePath).endsWith("doc.pdf");
        assertThat(Files.exists(tempDir.resolve(relativePath))).isTrue();
    }

    @Test
    void storeAddsSuffixOnFilenameCollision() {
        MultipartFile file1 = new MockMultipartFile("files", "doc.pdf", "application/pdf", "one".getBytes());
        MultipartFile file2 = new MockMultipartFile("files", "doc.pdf", "application/pdf", "two".getBytes());
        DocumentStorageService storage = service();

        String path1 = storage.store(file1, "Client", "Service");
        String path2 = storage.store(file2, "Client", "Service");

        assertThat(path1).isNotEqualTo(path2);
        assertThat(path2).contains("doc(1).pdf");
    }

    @Test
    void storeSanitizesIllegalCharactersInClientAndServiceNames() {
        MultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "x".getBytes());

        String relativePath = service().store(file, "Client: <Name>", "Service/Type");

        assertThat(relativePath).doesNotContain(":").doesNotContain("<").doesNotContain(">");
    }

    @Test
    void deletePrunesNowEmptyAncestorDirectoriesButStopsAtRoot() {
        MultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "x".getBytes());
        DocumentStorageService storage = service();
        String relativePath = storage.store(file, "Client", "Service");

        storage.delete(relativePath);

        assertThat(Files.exists(tempDir.resolve(relativePath))).isFalse();
        assertThat(Files.isDirectory(tempDir)).isTrue();
        try (var listing = Files.list(tempDir)) {
            assertThat(listing.findAny()).isEmpty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void deleteRejectsPathEscapingStorageRoot() {
        DocumentStorageService storage = service();

        assertThatThrownBy(() -> storage.delete("../../etc/passwd")).isInstanceOf(SecurityException.class);
    }

    @Test
    void loadAsResourceRejectsPathEscapingStorageRoot() {
        DocumentStorageService storage = service();

        assertThatThrownBy(() -> storage.loadAsResource("../../etc/passwd")).isInstanceOf(SecurityException.class);
    }

    @Test
    void loadAsResourceThrowsWhenFileMissing() {
        DocumentStorageService storage = service();

        assertThatThrownBy(() -> storage.loadAsResource("does/not/exist.pdf"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void loadAsResourceReturnsStoredFile() {
        MultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "hello".getBytes());
        DocumentStorageService storage = service();
        String relativePath = storage.store(file, "Client", "Service");

        Resource resource = storage.loadAsResource(relativePath);

        assertThat(resource.exists()).isTrue();
    }
}
