// Service containing business logic for QuizServiceTest
package org.hartford.iqsure.service;
import org.hartford.iqsure.dto.request.QuizRequestDTO;
import org.hartford.iqsure.dto.response.QuizResponseDTO;
import org.hartford.iqsure.entity.Quiz;
import org.hartford.iqsure.repository.QuestionRepository;
import org.hartford.iqsure.repository.QuizRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class QuizServiceTest {
    @Mock
    private QuizRepository quizRepositoryMock;
    @Mock
    private QuestionRepository questionRepositoryMock;
    @InjectMocks
    private QuizService quizService;
    @Test
    public void testGetQuizById() {
        Quiz quiz = Quiz.builder()
                .quizId(1L)
                .title("Health Quiz")
                .category("Health")
                .difficulty(Quiz.Difficulty.EASY)
                .build();
        when(quizRepositoryMock.findById(1L)).thenReturn(Optional.of(quiz));
        when(questionRepositoryMock.countByQuiz_QuizId(1L)).thenReturn(5L);
        QuizResponseDTO result = quizService.getQuizById(1L);
        assertNotNull(result);
        assertEquals("Health Quiz", result.getTitle());
        assertEquals(5, result.getTotalQuestions());
        verify(quizRepositoryMock).findById(1L);
    }
    @Test
    public void testCreateQuiz() {
        QuizRequestDTO request = new QuizRequestDTO();
        request.setTitle("New Quiz");
        request.setCategory("General");
        request.setDifficulty(Quiz.Difficulty.HARD);
        Quiz savedQuiz = Quiz.builder()
                .quizId(2L)
                .title("New Quiz")
                .category("General")
                .difficulty(Quiz.Difficulty.HARD)
                .build();
        when(quizRepositoryMock.save(any(Quiz.class))).thenReturn(savedQuiz);
        when(questionRepositoryMock.countByQuiz_QuizId(2L)).thenReturn(0L);
        QuizResponseDTO result = quizService.createQuiz(request);
        assertNotNull(result);
        assertEquals("New Quiz", result.getTitle());
        verify(quizRepositoryMock).save(any(Quiz.class));
    }
}