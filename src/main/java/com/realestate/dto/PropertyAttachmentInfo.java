package com.realestate.dto;
import java.time.Instant;
public record PropertyAttachmentInfo(String id, String fileName, long size, Instant uploadedAt) {}
