package org.hartford.iqsure.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.dto.response.QuizResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Static Quiz operations (Now mostly handled by AI Academy)")
public class QuizController {
    @GetMapping
    @Operation(summary = "Get all quizzes (Returns empty as AI Academy is dynamic)")
    public ResponseEntity<List<QuizResponseDTO>> getAll() {
        return ResponseEntity.ok(Collections.emptyList());
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get quiz by ID (Returns 404 as static quizzes are migrated)")
    public ResponseEntity<QuizResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.notFound().build();
    }
}
