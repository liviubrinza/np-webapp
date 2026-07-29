package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Document;
import com.brinza.notary.domain.InternalNote;
import com.brinza.notary.dto.DocumentView;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private ServiceCatalogService serviceCatalogService;

    private DocumentManagementService service() {
        return new DocumentManagementService(documentRepository, appointmentRepository, documentStorageService, serviceCatalogService);
    }

    @Test
    void listForAppointmentMapsToViews() {
        Document document = new Document("t", null, "path", "file.pdf", "application/pdf", null);
        when(documentRepository.findByAppointmentIdOrderByUploadedAtAsc(1L)).thenReturn(List.of(document));

        List<DocumentView> views = service().listForAppointment(1L);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).originalFilename()).isEqualTo("file.pdf");
    }

    @Test
    void uploadRejectsWhenNoFilesSelected() {
        assertThatThrownBy(() -> service().upload(1L, List.of(), "titi"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadRejectsWhenAllFilesEmpty() {
        MultipartFile empty = new MockMultipartFile("files", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service().upload(1L, List.of(empty), "titi"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadRejectsDisallowedExtension() {
        MultipartFile exe = new MockMultipartFile("files", "virus.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> service().upload(1L, List.of(exe), "titi"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(appointmentRepository, never()).findById(any());
    }

    @Test
    void uploadThrowsWhenAppointmentNotFound() {
        MultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "x".getBytes());
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().upload(1L, List.of(file), "titi"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void uploadStoresFilesAndAddsInternalNote() {
        MultipartFile file = new MockMultipartFile("files", "doc.pdf", "application/pdf", "x".getBytes());
        Appointment appointment = mock(Appointment.class);
        when(appointment.getClientName()).thenReturn("Ion Popescu");
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(serviceCatalogService.resolveName(any(), eq(Locale.ENGLISH))).thenReturn("Authentication");
        when(documentStorageService.store(file, "Ion Popescu", "Authentication")).thenReturn("2026/doc.pdf");

        service().upload(1L, List.of(file), "titi");

        verify(documentRepository).save(any(Document.class));
        ArgumentCaptor<InternalNote> noteCaptor = ArgumentCaptor.forClass(InternalNote.class);
        verify(appointment).addInternalNote(noteCaptor.capture());
        assertThat(noteCaptor.getValue().getNote()).contains("doc.pdf");
        assertThat(noteCaptor.getValue().getAuthorUsername()).isEqualTo("titi");
    }

    @Test
    void downloadUsesDocumentContentType() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(5L);
        Document document = new Document("t", null, "path/file.pdf", "file.pdf", "application/pdf", appointment);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        Resource resource = mock(Resource.class);
        when(documentStorageService.loadAsResource("path/file.pdf")).thenReturn(resource);

        ResponseEntity<Resource> response = service().download(5L, 10L);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("file.pdf");
    }

    @Test
    void downloadFallsBackToOctetStreamWhenContentTypeMissing() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(5L);
        Document document = new Document("t", null, "path/file.bin", "file.bin", null, appointment);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentStorageService.loadAsResource("path/file.bin")).thenReturn(mock(Resource.class));

        ResponseEntity<Resource> response = service().download(5L, 10L);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Test
    void downloadThrowsWhenDocumentBelongsToDifferentAppointment() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(5L);
        Document document = new Document("t", null, "path", "file.pdf", "application/pdf", appointment);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service().download(99L, 10L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteRemovesFileEntityAndAddsNote() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(5L);
        Document document = new Document("t", null, "path/file.pdf", "file.pdf", "application/pdf", appointment);
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        service().delete(5L, 10L, "titi");

        verify(documentStorageService).delete("path/file.pdf");
        verify(documentRepository).delete(document);
        ArgumentCaptor<InternalNote> noteCaptor = ArgumentCaptor.forClass(InternalNote.class);
        verify(appointment).addInternalNote(noteCaptor.capture());
        assertThat(noteCaptor.getValue().getNote()).contains("file.pdf");
    }
}
