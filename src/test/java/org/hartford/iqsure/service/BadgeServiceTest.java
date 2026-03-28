// Service containing business logic for BadgeServiceTest
package org.hartford.iqsure.service;
import org.hartford.iqsure.dto.request.BadgeRequestDTO;
import org.hartford.iqsure.dto.response.BadgeResponseDTO;
import org.hartford.iqsure.entity.Badge;
import org.hartford.iqsure.entity.User;
import org.hartford.iqsure.entity.UserBadge;
import org.hartford.iqsure.exception.ResourceNotFoundException;
import org.hartford.iqsure.repository.BadgeRepository;
import org.hartford.iqsure.repository.UserBadgeRepository;
import org.hartford.iqsure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class BadgeServiceTest {
    @Mock
    private BadgeRepository badgeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;
    @InjectMocks
    private BadgeService badgeService;
    @Test
    public void testCreateBadge() {
        BadgeRequestDTO request = new BadgeRequestDTO();
        request.setName("Rookie");
        request.setReqPoints(100);
        Badge savedBadge = Badge.builder().badgeId(1L).name("Rookie").reqPoints(100).build();
        when(badgeRepository.save(any(Badge.class))).thenReturn(savedBadge);
        BadgeResponseDTO result = badgeService.createBadge(request);
        assertNotNull(result);
        assertEquals("Rookie", result.getName());
        verify(badgeRepository).save(any(Badge.class));
    }
    @Test
    public void testGetAllBadges() {
        Badge b1 = Badge.builder().badgeId(1L).build();
        when(badgeRepository.findAll()).thenReturn(List.of(b1));
        List<BadgeResponseDTO> result = badgeService.getAllBadges();
        assertEquals(1, result.size());
    }
    @Test
    public void testCheckAndAwardBadges_NewBadgeAwarded() {
        User user = User.builder().userId(1L).userPoints(150).build();
        Badge rookieBadge = Badge.builder().badgeId(1L).name("Rookie").reqPoints(100).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(badgeRepository.findByReqPointsLessThanEqual(150)).thenReturn(List.of(rookieBadge));
        when(userBadgeRepository.existsByUser_UserIdAndBadge_BadgeId(1L, 1L)).thenReturn(false);
        List<BadgeResponseDTO> awarded = badgeService.checkAndAwardBadges(1L);
        assertEquals(1, awarded.size());
        assertEquals("Rookie", awarded.get(0).getName());
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }
    @Test
    public void testCheckAndAwardBadges_NoNewBadge() {
        User user = User.builder().userId(1L).userPoints(50).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(badgeRepository.findByReqPointsLessThanEqual(50)).thenReturn(List.of());
        List<BadgeResponseDTO> awarded = badgeService.checkAndAwardBadges(1L);
        assertTrue(awarded.isEmpty());
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }
    @Test
    public void testDeleteBadge_NotFound_ThrowsException() {
        when(badgeRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> badgeService.deleteBadge(99L));
    }
}