// Controller handling UserPolicyController related API endpoints

package org.hartford.iqsure.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hartford.iqsure.dto.request.UserPolicyRequestDTO;
import org.hartford.iqsure.dto.response.PremiumBreakdownDTO;
import org.hartford.iqsure.dto.response.PremiumCalculationLogResponseDTO;
import org.hartford.iqsure.dto.response.UserPolicyResponseDTO;
import org.hartford.iqsure.service.PremiumCalculationService;
import org.hartford.iqsure.service.UserPolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.hartford.iqsure.dto.response.UserRewardResponseDTO;
@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
@Tag(name = "User Policies & Premium", description = "Policy purchase and dynamic premium calculation")
public class UserPolicyController {
    private final UserPolicyService userPolicyService;
    private final PremiumCalculationService premiumCalculationService;
    @PostMapping("/policies")
    @Operation(summary = "Purchase a policy (applies gamification discounts automatically)")
    public ResponseEntity<UserPolicyResponseDTO> purchasePolicy(
            @PathVariable Long userId,
            @Valid @RequestBody UserPolicyRequestDTO dto,
            @RequestParam(required = false) List<Long> queryRewardIds) {
        List<Long> combinedRewards = new ArrayList<>();
        if (dto.getRewardIds() != null) combinedRewards.addAll(dto.getRewardIds());
        if (queryRewardIds != null) combinedRewards.addAll(queryRewardIds);
        List<Long> finalRewards = combinedRewards.stream().distinct().toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userPolicyService.purchasePolicy(userId, dto, finalRewards));
    }
    @GetMapping("/policies")
    @Operation(summary = "Get all policies purchased by a user")
    public ResponseEntity<List<UserPolicyResponseDTO>> getUserPolicies(@PathVariable Long userId) {
        return ResponseEntity.ok(userPolicyService.getUserPolicies(userId));
    }
    @GetMapping("/policies/{userPolicyId}")
    @Operation(summary = "Get a specific purchased policy by ID")
    public ResponseEntity<UserPolicyResponseDTO> getUserPolicyById(
            @PathVariable Long userId,
            @PathVariable Long userPolicyId) {
        return ResponseEntity.ok(userPolicyService.getUserPolicyById(userId, userPolicyId));
    }
    @GetMapping("/premium/calculate/{policyId}")
    @Operation(summary = "Preview dynamic premium for a policy before purchasing",
               description = "Evaluates all discount rules based on user's gamification data " +
                             "(points, badges, quiz scores and selected coupon rewards) and returns the full breakdown.")
    public ResponseEntity<PremiumBreakdownDTO> calculatePremium(
            @PathVariable Long userId,
            @PathVariable Long policyId,
            @RequestParam(required = false) List<Long> selectedRewardIds) {
        return ResponseEntity.ok(premiumCalculationService.calculatePremium(userId, policyId, selectedRewardIds));
    }
    @GetMapping("/premium/available-rewards")
    @Operation(summary = "Get user's available (unused) redeemed rewards for coupon selection")
    public ResponseEntity<List<UserRewardResponseDTO>> getAvailableRewards(@PathVariable Long userId) {
        return ResponseEntity.ok(premiumCalculationService.getAvailableRewardsForUser(userId));
    }
    @GetMapping("/premium/logs")
    @Operation(summary = "Get all premium calculation history for a user")
    public ResponseEntity<List<PremiumCalculationLogResponseDTO>> getPremiumLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(premiumCalculationService.getLogsForUser(userId));
    }
    @GetMapping("/premium/logs/{policyId}")
    @Operation(summary = "Get premium calculation history for a user on a specific policy")
    public ResponseEntity<List<PremiumCalculationLogResponseDTO>> getPremiumLogsByPolicy(
            @PathVariable Long userId,
            @PathVariable Long policyId) {
        return ResponseEntity.ok(premiumCalculationService.getLogsForUserAndPolicy(userId, policyId));
    }
    @PutMapping("/policies/{userPolicyId}/pay")
    @Operation(summary = "Pay for a policy quote and activate it")
    public ResponseEntity<UserPolicyResponseDTO> payPolicy(
            @PathVariable Long userId,
            @PathVariable Long userPolicyId) {
        return ResponseEntity.ok(userPolicyService.payPolicy(userPolicyId));
    }
}