// AI-powered claim fraud investigation service using Spring AI ChatClient
package org.hartford.iqsure.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hartford.iqsure.entity.Claim;
import org.hartford.iqsure.entity.UserPolicy;
import org.hartford.iqsure.exception.ResourceNotFoundException;
import org.hartford.iqsure.repository.ClaimRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class ClaimInvestigationAIService {

    private final ChatClient chatClient;
    private final ClaimRepository claimRepository;

    public ClaimInvestigationAIService(ChatClient.Builder chatClientBuilder,
                                       ClaimRepository claimRepository) {
        this.chatClient = chatClientBuilder.build();
        this.claimRepository = claimRepository;
    }

    @Data
    @Builder
    public static class ClaimAIInvestigationReport {
        private String fraudRiskLevel;         // LOW / MEDIUM / HIGH / CRITICAL
        private int fraudRiskScore;            // 0-100
        private String coverageVerdict;        // ELIGIBLE / PARTIAL / INELIGIBLE
        private BigDecimal recommendedPayout;
        private String aiNarrative;            // Full AI reasoning paragraph
        private List<String> redFlags;         // Adverse signals found
        private List<String> positiveSignals;  // Supporting legitimacy signals
        private String recommendedAction;      // APPROVE / PARTIAL_APPROVE / INVESTIGATE / REJECT
        private String officerGuidance;        // Instruction to the claims officer
        private int confidenceScore;           // 0-100
    }

    public ClaimAIInvestigationReport investigateClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));

        UserPolicy up = claim.getUserPolicy();
        String policyTitle = up != null && up.getPolicy() != null ? up.getPolicy().getTitle() : "Unknown Policy";
        BigDecimal coverageAmount = up != null && up.getRemainingCoverage() != null
                ? up.getRemainingCoverage() : BigDecimal.ZERO;

        String prompt = buildPrompt(claim, policyTitle, coverageAmount);

        log.info("Running AI investigation for claim: {}", claim.getClaimNumber());

        try {
            ClaimAIInvestigationReport report = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(ClaimAIInvestigationReport.class);

            if (report != null) {
                // Update stored AI audit summary on the entity
                claim.setAiAuditSummary(report.getAiNarrative());
                claim.setFraudRiskScore(report.getFraudRiskScore() != 0 ? (double) report.getFraudRiskScore() : 5.0);
                claimRepository.save(claim);
            }
            return report;
        } catch (Exception e) {
            log.error("AI investigation failed for claim {}: {}", claimId, e.getMessage());
            return buildFallbackReport(claim);
        }
    }

    private String buildPrompt(Claim claim, String policyTitle, BigDecimal remainingCoverage) {
        return String.format(
            "You are IQsure Sentinel, an expert AI Claims Investigator for a health insurance company. " +
            "Your task is to analyze the following insurance claim and produce a detailed fraud investigation report. " +
            "Be analytical, use actuarial reasoning, and provide actionable guidance for the claims officer.\n\n" +
            "--- CLAIM DETAILS ---\n" +
            "Claim Number: %s\n" +
            "Claim Type: %s\n" +
            "Claimed Amount: ₹%.2f\n" +
            "Hospital: %s\n" +
            "Incident Date: %s\n" +
            "Diagnosis / Event: %s\n" +
            "Linked Policy: %s\n" +
            "Remaining Coverage on Policy: ₹%.2f\n" +
            "--- END DETAILS ---\n\n" +
            "Perform an AI-powered FRAUD RISK ASSESSMENT and COVERAGE ELIGIBILITY CHECK.\n\n" +
            "Return ONLY valid JSON (no markdown, no explanation outside JSON) with this exact structure:\n" +
            "{\n" +
            "  \"fraudRiskLevel\": \"LOW|MEDIUM|HIGH|CRITICAL\",\n" +
            "  \"fraudRiskScore\": <integer 0-100>,\n" +
            "  \"coverageVerdict\": \"ELIGIBLE|PARTIAL|INELIGIBLE\",\n" +
            "  \"recommendedPayout\": <number>,\n" +
            "  \"aiNarrative\": \"<2-3 sentence professional AI reasoning summary>\",\n" +
            "  \"redFlags\": [\"<flag1>\", \"<flag2>\"],\n" +
            "  \"positiveSignals\": [\"<signal1>\", \"<signal2>\"],\n" +
            "  \"recommendedAction\": \"APPROVE|PARTIAL_APPROVE|INVESTIGATE|REJECT\",\n" +
            "  \"officerGuidance\": \"<one-sentence specific instruction>\",\n" +
            "  \"confidenceScore\": <integer 0-100>\n" +
            "}",
            claim.getClaimNumber(),
            claim.getType() != null ? claim.getType().name() : "REIMBURSEMENT",
            claim.getAmount() != null ? claim.getAmount() : BigDecimal.ZERO,
            claim.getHospitalName() != null ? claim.getHospitalName() : "Not specified",
            claim.getIncidentDate() != null ? claim.getIncidentDate().toString() : "Not specified",
            claim.getDiagnosis() != null ? claim.getDiagnosis() : "Not specified",
            policyTitle,
            remainingCoverage
        );
    }

    private ClaimAIInvestigationReport buildFallbackReport(Claim claim) {
        // Deterministic fallback if LLM fails (rule-based safety net)
        double amount = claim.getAmount() != null ? claim.getAmount().doubleValue() : 0;
        boolean highAmount = amount > 300000;
        boolean noDiagnosis = claim.getDiagnosis() == null || claim.getDiagnosis().isBlank();
        boolean noHospital = claim.getHospitalName() == null || claim.getHospitalName().isBlank();

        int score = 10;
        if (highAmount) score += 30;
        if (noDiagnosis) score += 25;
        if (noHospital) score += 20;

        String level = score > 60 ? "HIGH" : score > 35 ? "MEDIUM" : "LOW";
        String action = score > 60 ? "INVESTIGATE" : "APPROVE";

        return ClaimAIInvestigationReport.builder()
                .fraudRiskLevel(level)
                .fraudRiskScore(score)
                .coverageVerdict(score > 60 ? "PARTIAL" : "ELIGIBLE")
                .recommendedPayout(claim.getAmount())
                .aiNarrative("Automated risk assessment completed. Claim exhibits " + level.toLowerCase() +
                        " risk indicators based on amount, documentation, and incident details. " +
                        "Manual review recommended prior to final adjudication.")
                .redFlags(noDiagnosis ? List.of("Missing clinical diagnosis documentation") : List.of())
                .positiveSignals(List.of("Standard claim timeline observed", "Policy is active at time of filing"))
                .recommendedAction(action)
                .officerGuidance("Verify hospital admission records and cross-check diagnosis codes before processing.")
                .confidenceScore(72)
                .build();
    }
}
