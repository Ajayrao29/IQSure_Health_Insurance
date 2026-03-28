// Controller handling BadgeControllerTest related API endpoints
package org.hartford.iqsure.controller;
import org.hartford.iqsure.dto.request.BadgeRequestDTO;
import org.hartford.iqsure.dto.response.BadgeResponseDTO;
import org.hartford.iqsure.service.BadgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class BadgeControllerTest {
    @Mock
    private BadgeService badgeService;
    @InjectMocks
    private BadgeController badgeController;
    @Test
    public void testCreate() {
        BadgeRequestDTO request = new BadgeRequestDTO();
        request.setName("Rookie");
        BadgeResponseDTO responseDTO = BadgeResponseDTO.builder().badgeId(1L).name("Rookie").build();
        when(badgeService.createBadge(any(BadgeRequestDTO.class))).thenReturn(responseDTO);
        ResponseEntity<BadgeResponseDTO> response = badgeController.create(request);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("Rookie", response.getBody().getName());
        verify(badgeService).createBadge(any(BadgeRequestDTO.class));
    }
    @Test
    public void testGetAll() {
        BadgeResponseDTO dto = BadgeResponseDTO.builder().badgeId(1L).name("All").build();
        when(badgeService.getAllBadges()).thenReturn(List.of(dto));
        ResponseEntity<List<BadgeResponseDTO>> response = badgeController.getAll();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }
    @Test
    public void testGetByUser() {
        BadgeResponseDTO dto = BadgeResponseDTO.builder().badgeId(1L).name("UserBadge").build();
        when(badgeService.getBadgesByUser(1L)).thenReturn(List.of(dto));
        ResponseEntity<List<BadgeResponseDTO>> response = badgeController.getByUser(1L);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("UserBadge", response.getBody().get(0).getName());
    }
    @Test
    public void testDelete() {
        ResponseEntity<Void> response = badgeController.delete(1L);
        assertEquals(204, response.getStatusCode().value());
        verify(badgeService).deleteBadge(1L);
    }
}