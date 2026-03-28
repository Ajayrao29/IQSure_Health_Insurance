// Controller handling QuizControllerTest related API endpoints
package org.hartford.iqsure.controller;
import org.hartford.iqsure.dto.response.QuizResponseDTO;
import org.hartford.iqsure.service.QuizService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class QuizControllerTest {
    @Mock
    private QuizService quizServiceMock;
    @InjectMocks
    private QuizController quizController;
    @Test
    public void testGetAllQuizzes() {
        QuizResponseDTO quizDTO = QuizResponseDTO.builder()
                .quizId(1L)
                .title("Sample Quiz")
                .build();
        when(quizServiceMock.getAllQuizzes()).thenReturn(java.util.List.of(quizDTO));
        ResponseEntity<List<QuizResponseDTO>> response = quizController.getAll(null);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Sample Quiz", response.getBody().get(0).getTitle());
        verify(quizServiceMock).getAllQuizzes();
    }
    @Test
    public void testGetById() {
        QuizResponseDTO quizDTO = QuizResponseDTO.builder()
                .quizId(1L)
                .title("Sample Quiz")
                .build();
        when(quizServiceMock.getQuizById(1L)).thenReturn(quizDTO);
        ResponseEntity<QuizResponseDTO> response = quizController.getById(1L);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Sample Quiz", response.getBody().getTitle());
        verify(quizServiceMock).getQuizById(1L);
    }
}