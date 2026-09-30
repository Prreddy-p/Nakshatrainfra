package com.realestate.controller;

import com.realestate.repository.PropertyAttachmentRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/public/property-photos")
public class PublicPropertyPhotoController {
    private final PropertyAttachmentRepository attachments;
    public PublicPropertyPhotoController(PropertyAttachmentRepository attachments) { this.attachments = attachments; }
    private boolean photoName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png|gif|webp|heic)$");
    }
    public record Photo(String id, String url) {}
    @GetMapping
    public List<Photo> list() {
        return attachments.galleryCandidates().stream().filter(file -> photoName(file.fileName()))
            .map(file -> new Photo(file.id(), "/api/public/property-photos/" + file.id())).toList();
    }
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> image(@PathVariable String id) {
        var file = attachments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!photoName(file.getFileName())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        byte[] bytes = file.getContent();
        String type = imageType(bytes);
        if (type == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", "default-src 'none'; sandbox")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentLength(bytes.length).body(bytes);
    }
    private String imageType(byte[] bytes) {
        if (bytes.length < 12) return null;
        if ((bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        if (Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        String start = new String(bytes, 0, 12, StandardCharsets.ISO_8859_1);
        if (start.startsWith("GIF87a") || start.startsWith("GIF89a")) return "image/gif";
        if (start.startsWith("RIFF") && start.endsWith("WEBP")) return "image/webp";
        if (start.substring(4, 8).equals("ftyp") && Set.of("heic", "heix", "hevc", "hevx").contains(start.substring(8, 12))) return "image/heic";
        return null;
    }
}
