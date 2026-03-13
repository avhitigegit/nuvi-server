package com.nuvi.online_renting.common.validation;

import com.nuvi.online_renting.common.exceptions.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Set;

/**
 * Centralised file validation — used by every upload endpoint.
 *
 * Two-layer defence:
 *   1. Extension whitelist  — rejects obviously wrong file names
 *   2. Magic-byte inspection — reads the actual file content to confirm the
 *      declared type, preventing renamed executables being accepted.
 *
 * Supported upload types:
 *   Images    (item photos)   → jpg, jpeg, png, gif, webp
 *   Documents (KYC / seller)  → pdf, jpg, jpeg, png
 */
@Component
public class FileValidator {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final Set<String> ALLOWED_DOC_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png");

    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;  // 10 MB
    private static final long MAX_DOC_BYTES   = 20 * 1024 * 1024;  // 20 MB

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Validates an item image upload.
     * Allowed: jpg, jpeg, png, gif, webp — max 10 MB.
     *
     * @throws BadRequestException if the file fails any check
     */
    public void validateImage(MultipartFile file) {
        String ext = extractExtension(file);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new BadRequestException(
                    "Invalid file type '" + ext + "'. Allowed image formats: jpg, jpeg, png, gif, webp.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image exceeds the 10 MB size limit.");
        }
        String detected = detectMimeFromContent(file);
        if (detected == null || !detected.startsWith("image/")) {
            throw new BadRequestException(
                    "File content does not match an image type. Upload aborted to prevent malicious file uploads.");
        }
    }

    /**
     * Validates a KYC / seller document upload.
     * Allowed: pdf, jpg, jpeg, png — max 20 MB.
     *
     * @throws BadRequestException if the file fails any check
     */
    public void validateDocument(MultipartFile file) {
        String ext = extractExtension(file);
        if (!ALLOWED_DOC_EXTENSIONS.contains(ext)) {
            throw new BadRequestException(
                    "Invalid file type '" + ext + "'. Allowed document formats: pdf, jpg, jpeg, png.");
        }
        if (file.getSize() > MAX_DOC_BYTES) {
            throw new BadRequestException("Document exceeds the 20 MB size limit.");
        }
        if (ext.equals("pdf")) {
            validatePdfMagicBytes(file);
        } else {
            String detected = detectMimeFromContent(file);
            if (detected == null || !detected.startsWith("image/")) {
                throw new BadRequestException(
                        "File content does not match the declared image type. Upload aborted.");
            }
        }
    }

    /**
     * Validates a list of documents — convenience wrapper for multi-file uploads.
     */
    public void validateDocuments(List<MultipartFile> files) {
        for (MultipartFile file : files) {
            validateDocument(file);
        }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Extracts and lowercases the file extension from the original filename.
     * Rejects files with no extension or a blank name.
     */
    private String extractExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank() || !original.contains(".")) {
            throw new BadRequestException("File must have a valid extension.");
        }
        return original.substring(original.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Uses Java's URLConnection to sniff the MIME type from the first bytes of the stream.
     * Handles JPEG, PNG, GIF reliably. Returns null if the type cannot be determined.
     */
    private String detectMimeFromContent(MultipartFile file) {
        try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
            return URLConnection.guessContentTypeFromStream(bis);
        } catch (IOException e) {
            throw new BadRequestException("Could not read file content for validation.");
        }
    }

    /**
     * PDFs are not reliably detected by URLConnection — check magic bytes directly.
     * All valid PDF files begin with the 4-byte sequence: %PDF (hex 25 50 44 46).
     */
    private void validatePdfMagicBytes(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = in.read(header);
            if (read < 4 || header[0] != 0x25 || header[1] != 0x50
                         || header[2] != 0x44 || header[3] != 0x46) {
                throw new BadRequestException(
                        "File content does not match a valid PDF. Upload aborted.");
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read file content for validation.");
        }
    }
}
