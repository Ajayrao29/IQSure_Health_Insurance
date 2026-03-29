// Controller for AI-powered document generation endpoints
package org.hartford.iqsure.controller;

import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.service.DocumentGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DocumentGenerationController {

    private final DocumentGenerationService documentGenerationService;

    /**
     * Generate an AI-crafted Health Declaration document from policy application form data.
     * Called from the Apply Policy form at Step 3 (Medical Declarations).
     * The generated text replaces the need to upload a PDF health report.
     */
    @PostMapping("/generate-health-declaration")
    public ResponseEntity<DocumentGenerationService.GeneratedDocument> generateHealthDeclaration(
            @RequestBody DocumentGenerationService.HealthDeclarationRequest request) {
        return ResponseEntity.ok(documentGenerationService.generateHealthDeclaration(request));
    }

    /**
     * Generate an AI-crafted Claim Submission Letter from claim form data.
     * Called from the File Claim form at Step 3 (Evidence step).
     * The generated document serves as the primary claim submission artifact.
     */
    @PostMapping("/generate-claim-letter")
    public ResponseEntity<DocumentGenerationService.GeneratedDocument> generateClaimLetter(
            @RequestBody DocumentGenerationService.ClaimSummaryRequest request) {
        return ResponseEntity.ok(documentGenerationService.generateClaimSummaryLetter(request));
    }
}
