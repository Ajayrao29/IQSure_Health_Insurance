// Controller handling EducationContentControllerTest related API endpoints
package org.hartford.iqsure.controller;
import org.hartford.iqsure.dto.response.EducationContentDTO;
import org.hartford.iqsure.service.EducationContentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class EducationContentControllerTest {
    @Mock
    private EducationContentService educationContentServiceMock;
    @InjectMocks
    private EducationContentController educationContentController;
    @Test
    public void testGetByLanguage() {
        EducationContentDTO contentDTO = EducationContentDTO.builder()
                .title("Mock Title")
                .language("en")
                .build();
        when(educationContentServiceMock.getByLanguage("en")).thenReturn(java.util.List.of(contentDTO));
        ResponseEntity<List<EducationContentDTO>> response = educationContentController.getByLanguage("en");
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Mock Title", response.getBody().get(0).getTitle());
        verify(educationContentServiceMock).getByLanguage("en");
    }
    @Test
    public void testGetByTopicAndLanguage() {
        EducationContentDTO contentDTO = EducationContentDTO.builder()
                .topic("insurance")
                .title("Insurance Basics")
                .build();
        when(educationContentServiceMock.getByTopicAndLanguage("insurance", "en")).thenReturn(contentDTO);
        ResponseEntity<EducationContentDTO> response = educationContentController.getByTopicAndLanguage("insurance", "en");
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Insurance Basics", response.getBody().getTitle());
        verify(educationContentServiceMock).getByTopicAndLanguage("insurance", "en");
    }
}