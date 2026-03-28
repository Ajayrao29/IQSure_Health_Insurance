// Controller handling DiscountRuleControllerTest related API endpoints
package org.hartford.iqsure.controller;
import org.hartford.iqsure.dto.request.DiscountRuleRequestDTO;
import org.hartford.iqsure.dto.response.DiscountRuleResponseDTO;
import org.hartford.iqsure.service.DiscountRuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class DiscountRuleControllerTest {
    @Mock
    private DiscountRuleService discountRuleService;
    @InjectMocks
    private DiscountRuleController discountRuleController;
    @Test
    public void testGetActiveRules() {
        DiscountRuleResponseDTO dto = DiscountRuleResponseDTO.builder().ruleId(1L).ruleName("Active").build();
        when(discountRuleService.getActiveRules()).thenReturn(List.of(dto));
        ResponseEntity<List<DiscountRuleResponseDTO>> response = discountRuleController.getActiveRules();
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(discountRuleService).getActiveRules();
    }
    @Test
    public void testCreate() {
        DiscountRuleRequestDTO request = new DiscountRuleRequestDTO();
        request.setRuleName("New Rule");
        DiscountRuleResponseDTO responseDTO = DiscountRuleResponseDTO.builder().ruleId(1L).ruleName("New Rule").build();
        when(discountRuleService.createRule(any(DiscountRuleRequestDTO.class))).thenReturn(responseDTO);
        ResponseEntity<DiscountRuleResponseDTO> response = discountRuleController.create(request);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("New Rule", response.getBody().getRuleName());
    }
    @Test
    public void testDelete() {
        ResponseEntity<Void> response = discountRuleController.delete(1L);
        assertEquals(204, response.getStatusCode().value());
        verify(discountRuleService).deleteRule(1L);
    }
}