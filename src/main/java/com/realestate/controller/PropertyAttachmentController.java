package com.realestate.controller;

import com.realestate.dto.PropertyAttachmentInfo;
import com.realestate.service.PropertyAttachmentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.io.IOException;

@RestController
@RequestMapping("/api/properties/{id}/attachments")
public class PropertyAttachmentController {
    private final PropertyAttachmentService service;
    public PropertyAttachmentController(PropertyAttachmentService service) { this.service = service; }
    @GetMapping public List<PropertyAttachmentInfo> list(@PathVariable Long id) { return service.list(id); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<PropertyAttachmentInfo> upload(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) throws IOException {
        return service.upload(id, files);
    }
    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> download(@PathVariable Long id, @PathVariable String attachmentId) {
        var file = service.download(id, attachmentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name(), StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentLength(file.content().length).body(file.content());
    }
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable String attachmentId) {
        service.delete(id, attachmentId); return ResponseEntity.noContent().build();
    }
}
