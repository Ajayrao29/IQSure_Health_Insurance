package org.hartford.iqsure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hartford.iqsure.dto.response.EducationContentDTO;
import org.hartford.iqsure.dto.response.QuestionResponseDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIAcademyService {

    private final ChatClient chatClient;
    private final org.hartford.iqsure.repository.UserRepository userRepository;
    private final org.hartford.iqsure.repository.AttemptRepository attemptRepository;
    private final org.hartford.iqsure.service.BadgeService badgeService;

    public AIAcademyService(ChatClient.Builder chatClientBuilder, 
                            org.hartford.iqsure.repository.UserRepository userRepository,
                            org.hartford.iqsure.service.BadgeService badgeService,
                            org.hartford.iqsure.repository.AttemptRepository attemptRepository) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.badgeService = badgeService;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Awards loyalty points for completing an academy session.
     */
    @org.springframework.transaction.annotation.Transactional
    public void rewardCompletion(Long userId, String topic, int score, int total) {
        org.hartford.iqsure.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // 1. Basic Reward
        int bonus = (score * 10); // Example: 5 correct * 10 = 50 points
        user.setUserPoints(user.getUserPoints() + bonus); 
        user.setTotalQuizzesTaken(user.getTotalQuizzesTaken() + 1);

        // 2. Streak Calculation
        java.time.LocalDate today = java.time.LocalDate.now();
        if (user.getLastQuizDate() == null) {
            user.setCurrentStreak(1);
        } else {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(user.getLastQuizDate(), today);
            if (daysBetween == 1) {
                user.setCurrentStreak(user.getCurrentStreak() + 1);
            } else if (daysBetween > 1) {
                user.setCurrentStreak(1);
            }
        }
        user.setLastQuizDate(today);
        userRepository.save(user);

        // 3. Record History (Attempt)
        org.hartford.iqsure.entity.Attempt attempt = org.hartford.iqsure.entity.Attempt.builder()
                .user(user)
                .quizTitle(topic)
                .score(score)
                .totalQuestions(total)
                .percentage((int) ((double) score / total * 100))
                .pointsEarned(bonus)
                .build();
        attemptRepository.save(attempt);

        // 4. Badge Progression
        badgeService.checkAndAwardBadges(userId);
        
        log.info("Recorded attempt for {}: Score {}/{} -> {} points", userId, score, total, bonus);
    }

    /**
     * Generates a professional insurance lesson based on a topic and language.
     */
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

    /**
     * Answers follow-up doubts about a specific lesson context.
     */
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

    /**
     * Generates a quiz based on the provided session content.
     */
    public List<QuestionResponseDTO> generateQuiz(String context, String language) {
        String prompt = String.format(
            "Based on the following insurance lesson, generate 5 challenging multiple-choice questions in %s. " +
            "MANDATORY: Return the response ONLY as a JSON array of objects. " +
            "Each object MUST have: 'text' (question string), 'options' (list of exactly 4 strings), and 'correctOptionIndex' (0-3). " +
            "No introductory text or conversational fillers. JSON only. " +
            "Context: %s",
            language, context
        );

        log.info("Generating AI quiz for context in {}", language);

        List<AICalibratedQuestion> aiQuestions = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<List<AICalibratedQuestion>>() {});

        if (aiQuestions == null) return List.of();

        return aiQuestions.stream()
                .map(q -> QuestionResponseDTO.builder()
                        .text(q.text)
                        .options(q.options)
                        .correctOptionIndex(q.correctOptionIndex)
                        .build())
                .collect(Collectors.toList());
    }

    // Temporary internal DTO for AI parsing
    public static class AICalibratedQuestion {
        public String text;
        public List<String> options;
        public int correctOptionIndex;
    }
}




