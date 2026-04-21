package com.cns.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*")
public class PdfController {

    @Autowired
    private HashService hashService;

    @Autowired
    private PdfSignerService pdfSignerService;

    @Autowired
    private PdfVerifierService pdfVerifierService;

    @PostMapping("/hash")
    public ResponseEntity<Map<String, String>> getPdfHash(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files allowed."));
            }
            String hash = hashService.generatePdfHash(file.getInputStream());
            return ResponseEntity.ok(Map.of("hash", hash));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sign-pdf")
    public ResponseEntity<byte[]> signPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("signerName") String signerName) {
        try {
            byte[] signedPdf = pdfSignerService.signPdf(file.getInputStream(), signerName);

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"signed_" + file.getOriginalFilename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(signedPdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/verify-pdf")
    public ResponseEntity<Map<String, Object>> verifyPdf(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(pdfVerifierService.verifyPdf(file.getInputStream()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
