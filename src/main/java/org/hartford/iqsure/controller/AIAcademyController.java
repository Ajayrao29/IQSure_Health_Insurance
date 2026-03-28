// Controller handling AIAcademyController related API endpoints
package org.hartford.iqsure.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.dto.response.EducationContentDTO;
import org.hartford.iqsure.dto.response.QuestionResponseDTO;
import org.hartford.iqsure.service.AIAcademyService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
@RestController
@RequestMapping("/api/ai-academy")
@RequiredArgsConstructor
@Tag(name = "AI Academy Oracle", description = "On-demand educational content using Spring AI (Groq/OpenAI)")
@CrossOrigin("*")
public class AIAcademyController {
    private final AIAcademyService aiAcademyService;
    private final org.hartford.iqsure.service.UserService userService;
    @GetMapping("/generate-lesson")
    @Operation(summary = "Generate a professional insurance lesson for a specific topic")
    public EducationContentDTO getLesson(@RequestParam String topic, @RequestParam(defaultValue = "English") String lang) {
        return aiAcademyService.generateLesson(topic, lang);
    }
    @PostMapping("/generate-quiz")
    @Operation(summary = "Generate a dynamic assessment based on lesson context")
    public List<QuestionResponseDTO> getQuiz(@RequestBody String context, @RequestParam(defaultValue = "English") String lang) {
        return aiAcademyService.generateQuiz(context, lang);
    }
    @PostMapping("/ask-follow-up")
    @Operation(summary = "Ask a follow-up doubt about the current lesson context")
    public String askFollowUp(
            @RequestParam String context,
            @RequestParam String doubt,
            @RequestParam(defaultValue = "English") String lang) {
        return aiAcademyService.generateFollowUp(context, doubt, lang);
    }
    @PostMapping("/complete-lesson")
    @Operation(summary = "Record completion of an academy session and award loyalty points")
    public ResponseEntity<org.hartford.iqsure.dto.response.UserResponseDTO> completeLesson(
            @RequestParam Long userId,
            @RequestParam String topic,
            @RequestParam int score,
            @RequestParam int total) {
        aiAcademyService.rewardCompletion(userId, topic, score, total);
        return ResponseEntity.ok(userService.getProfile(userId));
    }
    @GetMapping("/tts")
    @Operation(summary = "Proxy for free Google TTS to avoid CORS/Rate issues")
    public ResponseEntity<byte[]> getTtsAudio(
            @RequestParam String text,
            @RequestParam String language) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlStr = "https://translate.googleapis.com/translate_tts?client=gtx&ie=UTF-8&tl=" + language + "&q=" + encodedText;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            InputStream is = conn.getInputStream();
            byte[] audioBytes = is.readAllBytes();
            is.close();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}