// Service containing business logic for AIAcademyService
package org.hartford.iqsure.service;
import lombok.extern.slf4j.Slf4j;
import org.hartford.iqsure.dto.response.EducationContentDTO;
import org.hartford.iqsure.dto.response.QuestionResponseDTO;
import org.hartford.iqsure.entity.Attempt;
import org.hartford.iqsure.entity.User;
import org.hartford.iqsure.exception.ResourceNotFoundException;
import org.hartford.iqsure.repository.AttemptRepository;
import org.hartford.iqsure.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
@Service
@Slf4j
public class AIAcademyService {
    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;
    private final BadgeService badgeService;
    public AIAcademyService(ChatClient.Builder chatClientBuilder,
                            UserRepository userRepository,
                            BadgeService badgeService,
                            AttemptRepository attemptRepository) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.badgeService = badgeService;
        this.attemptRepository = attemptRepository;
    }
    @Transactional
    public void rewardCompletion(Long userId, String topic, int score, int total) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        int bonus = (score * 10);
        user.setUserPoints(user.getUserPoints() + bonus);
        user.setTotalQuizzesTaken(user.getTotalQuizzesTaken() + 1);
        LocalDate today = LocalDate.now();
        if (user.getLastQuizDate() == null) {
            user.setCurrentStreak(1);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(user.getLastQuizDate(), today);
            if (daysBetween == 1) {
                user.setCurrentStreak(user.getCurrentStreak() + 1);
            } else if (daysBetween > 1) {
                user.setCurrentStreak(1);
            }
        }
        user.setLastQuizDate(today);
        userRepository.save(user);
        Attempt attempt = Attempt.builder()
                .user(user)
                .quizTitle(topic)
                .score(score)
                .totalQuestions(total)
                .percentage((int) ((double) score / total * 100))
                .pointsEarned(bonus)
                .build();
        attemptRepository.save(attempt);
        badgeService.checkAndAwardBadges(userId);
        log.info("Recorded academy completion for user {}: {} points earned", userId, bonus);
    }
    public EducationContentDTO generateLesson(String topic, String language) {
        String prompt = String.format(
            "You are an insurance expert with 30 years of experience. " +
            "Generate a professional, high-quality educational lesson for the topic: '%s' in language: '%s'. " +
            "Return the response in JSON format. The JSON block should have fields: 'title', 'topic', 'content'. " +
            "MANDATORY: Return ONLY the JSON object. Do not include any conversational fillers, markdown outside the content field, or introductory text. " +
            "The 'content' field should be detailed, structured with Markdown (Headers, bold, sections), and end with a 'Veteran Pro-Tip'.",
            topic, language
        );
        log.info("Generating AI lesson for topic: {} in {}", topic, language);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(EducationContentDTO.class);
    }
    public String generateFollowUp(String context, String doubt, String language) {
        String prompt = String.format(
            "You are an insurance mentor. The user just studied a lesson about: '%s'. " +
            "They have a follow-up doubt: '%s'. " +
            "Provide a clear, helpful, and professional answer in %s. " +
            "Explain in simple terms but maintain technical accuracy. " +
            "Use limited Markdown for bolding key terms.",
            context, doubt, language
        );
        log.info("Generating AI follow-up for doubt: {}", doubt);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
    public List<QuestionResponseDTO> generateQuiz(String context, String language) {
        String prompt = String.format(
            "Based on the following insurance lesson, generate 5 challenging multiple-choice questions in %s. " +
            "MANDATORY: Return the response ONLY as a JSON array of objects. " +
            "Each object MUST have: 'text' (question string), 'options' (list of exactly 4 strings), and 'correctOptionIndex' (0-3). " +
            "No introductory text or conversational fillers. JSON only. " +
            "Context: %s",
            language, context
        );
        log.info("Generating AI quiz in {}", language);
        List<AICalibratedQuestion> aiQuestions = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(new ParameterizedTypeReference<List<AICalibratedQuestion>>() {});
        if (aiQuestions == null) return List.of();
        return aiQuestions.stream()
                .map(q -> QuestionResponseDTO.builder()
                        .text(q.text)
                        .options(q.options)
                        .correctOptionIndex(q.correctOptionIndex)
                        .build())
                .collect(Collectors.toList());
    }
    public static class AICalibratedQuestion {
        public String text;
        public List<String> options;
        public int correctOptionIndex;
    }
}