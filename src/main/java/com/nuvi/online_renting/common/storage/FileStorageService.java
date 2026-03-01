package com.nuvi.online_renting.common.storage;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root = Paths.get("uploads");

    public List<String> storeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // Create directory if not exists
                Files.createDirectories(root);

                // Clean file name and prepend UUID
                String filename = UUID.randomUUID() + "_" +
                        StringUtils.cleanPath(file.getOriginalFilename());

                Path dest = root.resolve(filename);

                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                // Add public URL or relative path
                urls.add("/uploads/" + filename);

            } catch (IOException e) {
                throw new RuntimeException("Failed to store file " + file.getOriginalFilename(), e);
            }
        }

        return urls;
    }
}
