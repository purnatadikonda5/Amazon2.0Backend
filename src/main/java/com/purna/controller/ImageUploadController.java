package com.purna.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.purna.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * ImageUploadController
 * 
 * WHY USE THIS:
 * This acts as the exact equivalent to an Express "Multer" interceptor in Node.js.
 * The React frontend hits this endpoint with FormData (MultipartFile). We buffer the image, 
 * proxy it to Cloudinary, and return the immutable string URL back to the frontend to stitch 
 * inside the Listing/Product creation JSON payload!
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        
        // 1. Emptiness Check
        if (file.isEmpty()) {
            response.put("error", "File cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 2. MIME Type Security Check
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            response.put("error", "Invalid file type. Only image files (JPEG, PNG, WEBP, etc.) are permitted.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 3. Local Hard Limit Check (5 MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            response.put("error", "File size perfectly exceeds the 5MB maximum limit.");
            return ResponseEntity.status(413).body(response);
        }

        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            response.put("url", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Image upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
