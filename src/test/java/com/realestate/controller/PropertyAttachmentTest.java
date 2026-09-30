package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.model.UserAccount;
import com.realestate.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:attachment-test;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
 "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class PropertyAttachmentTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserAccountRepository users;
    MockHttpSession session(String role) {
        var user = new UserAccount(); user.setName("Uploader"); user.setRole(role);
        user.setEmailId(java.util.UUID.randomUUID() + "@example.com"); user.setPassword("unused"); users.save(user);
        var session = new MockHttpSession(); session.setAttribute("accountId", user.getId()); return session;
    }
    @Test void uploadDownloadValidationAccessAndDeletion() throws Exception {
        var owner = session("Associate"); var other = session("Associate"); var manager = session("Manager");
        var property = json.readTree(mvc.perform(post("/api/properties").session(owner).contentType("application/json")
            .content(json.writeValueAsString(Map.of("propertyName", "With files"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String base = "/api/properties/" + property.get("id").asLong();
        byte[] content = "%PDF-1.7 test document".getBytes(StandardCharsets.UTF_8);
        var pdf = new MockMultipartFile("files", "plan.pdf", "application/pdf", content);
        var imageBytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", imageBytes);
        var image = new MockMultipartFile("files", "photo.jpg", "image/jpeg", imageBytes.toByteArray());
        mvc.perform(multipart(base + "/attachments").file(pdf)).andExpect(status().isUnauthorized());
        mvc.perform(multipart(base + "/attachments").file(pdf).session(other)).andExpect(status().isNotFound());
        String response = mvc.perform(multipart(base + "/attachments").file(pdf).file(image).session(owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].content").doesNotExist()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(response).get(0).get("id").asText();
        String photoId = json.readTree(response).get(1).get("id").asText();
        mvc.perform(get("/api/public/property-photos")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(photoId))
            .andExpect(jsonPath("$[0].fileName").doesNotExist());
        mvc.perform(get("/api/public/property-photos/" + photoId)).andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg")).andExpect(content().bytes(imageBytes.toByteArray()));
        mvc.perform(get("/api/public/property-photos/" + id)).andExpect(status().isNotFound());
        mvc.perform(get(base + "/attachments/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get(base + "/attachments/" + id).session(owner)).andExpect(status().isOk())
            .andExpect(content().bytes(content)).andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get(base + "/attachments/" + id).session(manager)).andExpect(status().isOk());
        mvc.perform(get(base + "/attachments").session(other)).andExpect(status().isNotFound());
        mvc.perform(get(base + "/attachments/" + id).session(other)).andExpect(status().isNotFound());
        mvc.perform(delete(base + "/attachments/" + id).session(other)).andExpect(status().isNotFound());
        mvc.perform(put(base).session(owner).contentType("application/json")
            .content(json.writeValueAsString(Map.of("propertyName", "Edited with files"))))
            .andExpect(status().isOk());
        mvc.perform(get(base + "/attachments").session(owner)).andExpect(jsonPath("$.length()").value(2));
        // A mixed invalid batch must save nothing, even when its first file is valid.
        mvc.perform(multipart(base + "/attachments").file(pdf)
            .file(new MockMultipartFile("files", "bad.html", "text/html", new byte[]{1})).session(owner))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart(base + "/attachments")
            .file(new MockMultipartFile("files", "large.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1])).session(owner))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart(base + "/attachments")
            .file(new MockMultipartFile("files", "empty.pdf", "application/pdf", new byte[0])).session(owner))
            .andExpect(status().isBadRequest());
        mvc.perform(get(base + "/attachments").session(owner)).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(delete(base + "/attachments/" + id).session(owner)).andExpect(status().isNoContent());
        mvc.perform(get(base + "/attachments/" + id).session(owner)).andExpect(status().isNotFound());
        mvc.perform(delete(base).session(owner)).andExpect(status().isNoContent());
    }
}
