package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Document;
import com.brinza.notary.domain.InternalNote;
import com.brinza.notary.dto.DocumentView;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.DocumentRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;

@org.springframework.stereotype.Service
public class DocumentManagementService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final DocumentRepository documentRepository;
    private final AppointmentRepository appointmentRepository;
    private final DocumentStorageService documentStorageService;
    private final ServiceCatalogService serviceCatalogService;

    public DocumentManagementService(DocumentRepository documentRepository,
                                      AppointmentRepository appointmentRepository,
                                      DocumentStorageService documentStorageService,
                                      ServiceCatalogService serviceCatalogService) {
        this.documentRepository = documentRepository;
        this.appointmentRepository = appointmentRepository;
        this.documentStorageService = documentStorageService;
        this.serviceCatalogService = serviceCatalogService;
    }

    @Transactional(readOnly = true)
    public List<DocumentView> listForAppointment(Long appointmentId) {
        return documentRepository.findByAppointmentIdOrderByUploadedAtAsc(appointmentId).stream()
                .map(d -> new DocumentView(d.getId(), d.getOriginalFilename(), d.getUploadedAt()))
                .toList();
    }

    @Transactional
    public void upload(Long appointmentId, List<MultipartFile> files, String authorUsername) {
        List<MultipartFile> selected = files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Select at least one file to upload.");
        }
        for (MultipartFile file : selected) {
            if (!ALLOWED_EXTENSIONS.contains(extensionOf(file.getOriginalFilename()))) {
                throw new IllegalArgumentException(
                        "Unsupported file type: " + file.getOriginalFilename() + ". Only PDF, DOC, and DOCX files are allowed.");
            }
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + appointmentId));
        String serviceName = serviceCatalogService.resolveName(appointment.getService(), Locale.ENGLISH);

        List<String> uploadedNames = new ArrayList<>();
        for (MultipartFile file : selected) {
            String relativePath = documentStorageService.store(file, appointment.getClientName(), serviceName);
            Document document = new Document(file.getOriginalFilename(), null, relativePath,
                    file.getOriginalFilename(), file.getContentType(), appointment);
            documentRepository.save(document);
            uploadedNames.add(file.getOriginalFilename());
        }

        String note = "Uploaded document%s: %s".formatted(uploadedNames.size() > 1 ? "s" : "", String.join(", ", uploadedNames));
        appointment.addInternalNote(new InternalNote(authorUsername, note));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long appointmentId, Long documentId) {
        Document document = findForAppointment(appointmentId, documentId);

        Resource resource = documentStorageService.loadAsResource(document.getStoredPath());
        MediaType mediaType = document.getContentType() != null
                ? MediaType.parseMediaType(document.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(resource);
    }

    @Transactional
    public void delete(Long appointmentId, Long documentId, String authorUsername) {
        Document document = findForAppointment(appointmentId, documentId);
        Appointment appointment = document.getAppointment();
        String filename = document.getOriginalFilename();

        documentStorageService.delete(document.getStoredPath());
        documentRepository.delete(document);

        appointment.addInternalNote(new InternalNote(authorUsername, "Deleted document: " + filename));
    }

    private Document findForAppointment(Long appointmentId, Long documentId) {
        return documentRepository.findById(documentId)
                .filter(d -> d.getAppointment() != null && d.getAppointment().getId().equals(appointmentId))
                .orElseThrow(() -> new NoSuchElementException(
                        "No document with id " + documentId + " for appointment " + appointmentId));
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}
