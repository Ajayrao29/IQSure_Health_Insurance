package org.hartford.iqsure.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.dto.response.AttemptResponseDTO;
import org.hartford.iqsure.repository.AttemptRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attempts")
@RequiredArgsConstructor
@Tag(name = "Attempts", description = "User quiz attempt history and achievements")
@CrossOrigin("*")
public class AttemptController {

    private final AttemptRepository attemptRepository;

    @GetMapping
    @Operation(summary = "Get quiz attempt history for a specific user")
    public ResponseEntity<List<AttemptResponseDTO>> getAttemptsByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(attemptRepository.findByUser_UserIdOrderByAttemptDateDesc(userId)
                .stream()
                .map(a -> AttemptResponseDTO.builder()
                        .attemptId(a.getId())
                        .userId(a.getUser().getUserId())
                        .quizTitle(a.getQuizTitle())
                        .score(a.getScore())
                        .totalQuestions(a.getTotalQuestions())
                        .percentage(a.getPercentage())
                        .pointsEarned(a.getPointsEarned())
                        .attemptDate(a.getAttemptDate())
                        .build())
                .collect(Collectors.toList()));
    }
}
