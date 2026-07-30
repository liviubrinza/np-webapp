package com.brinza.notary.service;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.Document;
import com.brinza.notary.domain.InternalNote;
import com.brinza.notary.dto.DocumentView;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

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
        log.info("listForAppointment called for appointmentId={}", appointmentId);
        return documentRepository.findByAppointmentIdOrderByUploadedAtAsc(appointmentId).stream()
                .map(d -> new DocumentView(d.getId(), d.getOriginalFilename(), d.getUploadedAt()))
                .toList();
    }

    @Transactional
    public void upload(Long appointmentId, List<MultipartFile> files, String authorUsername) {
        log.info("upload called for appointmentId={} fileCount={} author={}",
                appointmentId, files == null ? 0 : files.size(), authorUsername);
        List<MultipartFile> selected = files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
        if (selected.isEmpty()) {
            log.debug("Rejected upload for appointmentId={}: no files selected", appointmentId);
            throw new IllegalArgumentException("Select at least one file to upload.");
        }
        for (MultipartFile file : selected) {
            String originalFilename = stripControlChars(file.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.contains(extensionOf(originalFilename))) {
                log.debug("Rejected upload for appointmentId={}: unsupported file type {}", appointmentId, originalFilename);
                throw new IllegalArgumentException(
                        "Unsupported file type: " + originalFilename + ". Only PDF, DOC, and DOCX files are allowed.");
            }
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NoSuchElementException("No appointment with id " + appointmentId));
        String serviceName = serviceCatalogService.resolveName(appointment.getService(), Locale.ENGLISH);

        List<String> uploadedNames = new ArrayList<>();
        for (MultipartFile file : selected) {
            String originalFilename = stripControlChars(file.getOriginalFilename());
            String relativePath = documentStorageService.store(file, appointment.getClientName(), serviceName);
            Document document = new Document(originalFilename, null, relativePath,
                    originalFilename, file.getContentType(), appointment);
            documentRepository.save(document);
            uploadedNames.add(originalFilename);
        }
        log.debug("Stored {} document(s) for appointmentId={}: {}", uploadedNames.size(), appointmentId, uploadedNames);

        String note = "Uploaded document%s: %s".formatted(uploadedNames.size() > 1 ? "s" : "", String.join(", ", uploadedNames));
        appointment.addInternalNote(new InternalNote(authorUsername, note));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long appointmentId, Long documentId) {
        log.info("download called for appointmentId={} documentId={}", appointmentId, documentId);
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
        log.info("delete called for appointmentId={} documentId={} author={}", appointmentId, documentId, authorUsername);
        Document document = findForAppointment(appointmentId, documentId);
        Appointment appointment = document.getAppointment();
        String filename = document.getOriginalFilename();

        documentStorageService.delete(document.getStoredPath());
        documentRepository.delete(document);
        log.debug("Deleted document id={} filename={}", documentId, filename);

        appointment.addInternalNote(new InternalNote(authorUsername, "Deleted document: " + filename));
    }

    private Document findForAppointment(Long appointmentId, Long documentId) {
        log.debug("findForAppointment called for appointmentId={} documentId={}", appointmentId, documentId);
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

    // The client-supplied filename is kept as-is for display (unlike the storage path, which
    // DocumentStorageService sanitizes separately) but flows into log lines, internal notes, and
    // the Content-Disposition header on download - stripping control characters here closes off
    // log-forging and HTTP header injection via a crafted upload filename at the one place it enters.
    private static String stripControlChars(String value) {
        return value == null ? null : value.replaceAll("[\\p{Cntrl}]", "");
    }
}
