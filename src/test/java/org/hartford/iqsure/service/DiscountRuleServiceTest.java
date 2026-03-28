// Service containing business logic for DiscountRuleServiceTest
package org.hartford.iqsure.service;
import org.hartford.iqsure.dto.request.DiscountRuleRequestDTO;
import org.hartford.iqsure.dto.response.DiscountRuleResponseDTO;
import org.hartford.iqsure.entity.DiscountRule;
import org.hartford.iqsure.exception.BadRequestException;
import org.hartford.iqsure.repository.DiscountRuleRepository;
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
public class DiscountRuleServiceTest {
    @Mock
    private DiscountRuleRepository discountRuleRepository;
    @InjectMocks
    private DiscountRuleService discountRuleService;
    @Test
    public void testCreateRule_Successful() {
        DiscountRuleRequestDTO dto = new DiscountRuleRequestDTO();
        dto.setRuleName("Loyalty");
        dto.setDiscountPercentage(10.0);
        DiscountRule saved = DiscountRule.builder().ruleId(1L).ruleName("Loyalty").build();
        when(discountRuleRepository.existsByRuleName("Loyalty")).thenReturn(false);
        when(discountRuleRepository.save(any(DiscountRule.class))).thenReturn(saved);
        DiscountRuleResponseDTO result = discountRuleService.createRule(dto);
        assertNotNull(result);
        assertEquals("Loyalty", result.getRuleName());
        verify(discountRuleRepository).save(any(DiscountRule.class));
    }
    @Test
    public void testCreateRule_Duplicate_ThrowsException() {
        DiscountRuleRequestDTO dto = new DiscountRuleRequestDTO();
        dto.setRuleName("Exists");
        when(discountRuleRepository.existsByRuleName("Exists")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> discountRuleService.createRule(dto));
    }
    @Test
    public void testGetAllRules() {
        DiscountRule r1 = DiscountRule.builder().ruleId(1L).build();
        when(discountRuleRepository.findAll()).thenReturn(List.of(r1));
        List<DiscountRuleResponseDTO> result = discountRuleService.getAllRules();
        assertEquals(1, result.size());
    }
}