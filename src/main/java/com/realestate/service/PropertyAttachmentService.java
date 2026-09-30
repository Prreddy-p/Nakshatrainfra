package com.realestate.service;

import com.realestate.dto.PropertyAttachmentInfo;
import com.realestate.model.PropertyAttachment;
import com.realestate.repository.PropertyAttachmentRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.security.RecordAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.util.*;

@Service
public class PropertyAttachmentService {
    private static final long MAX_FILE = 10 * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "odt", "ods", "zip");
    private final PropertyRepository properties;
    private final PropertyAttachmentRepository attachments;
    public PropertyAttachmentService(PropertyRepository properties, PropertyAttachmentRepository attachments) {
        this.properties = properties; this.attachments = attachments;
    }
    private com.realestate.model.Property property(Long id) {
        return RecordAccess.require(properties.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
    @Transactional(readOnly = true)
    public List<PropertyAttachmentInfo> list(Long id) { property(id); return attachments.list(id); }
    @Transactional(rollbackFor = IOException.class)
    public List<PropertyAttachmentInfo> upload(Long id, List<MultipartFile> files) throws IOException {
        var property = property(id);
        if (files.isEmpty() || files.size() > 10) throw new IllegalArgumentException("Choose between 1 and 10 files per upload.");
        long total = 0;
        List<String> names = new ArrayList<>();
        for (var file : files) {
            if (file.isEmpty() || file.getSize() > MAX_FILE) throw new IllegalArgumentException("Each file must be nonempty and no larger than 10 MB.");
            total += file.getSize();
            String name = Objects.toString(file.getOriginalFilename(), "").replace('\\', '/');
            name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_").trim();
            if (name.isEmpty() || name.length() > 255) throw new IllegalArgumentException("File names must contain 1 to 255 characters.");
            String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (!name.contains(".") || !EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Unsupported file type. Choose photos, PDFs, Office documents, text files, or ZIP files.");
            names.add(name);
        }
        if (total > 50 * 1024 * 1024) throw new IllegalArgumentException("Upload at most 50 MB at a time.");
        for (int i = 0; i < files.size(); i++) attachments.save(new PropertyAttachment(property, names.get(i), files.get(i).getBytes()));
        attachments.flush();
        return attachments.list(id);
    }
    public record Download(String name, byte[] content) {}
    @Transactional(readOnly = true)
    public Download download(Long id, String attachmentId) {
        property(id);
        var attachment = attachments.findByIdAndProperty_Id(attachmentId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Download(attachment.getFileName(), attachment.getContent());
    }
    @Transactional
    public void delete(Long id, String attachmentId) {
        property(id);
        var attachment = attachments.findByIdAndProperty_Id(attachmentId, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        attachments.delete(attachment);
    }
}
