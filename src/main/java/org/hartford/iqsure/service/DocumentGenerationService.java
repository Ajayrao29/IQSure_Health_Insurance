// AI-powered document generation service
// Generates personalized policy health declarations and claim summary letters
// using Spring AI ChatClient (Groq LLM)
package org.hartford.iqsure.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DocumentGenerationService {

    private final ChatClient chatClient;

    public DocumentGenerationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Request DTO for health declaration generation
     */
    @Data
    public static class HealthDeclarationRequest {
        private String applicantName;
        private String policyTitle;
        private String policyType;
        private Double coverageAmount;
        private Boolean hospitalizedLastYear;
        private Boolean chronicConditions;
        private Boolean smokeOrAlcohol;
        private Boolean surgicalHistory;
        private String nomineeName;
        private String nomineeRelationship;
        private List<MemberSummary> members;
    }

    @Data
    public static class MemberSummary {
        private String fullName;
        private String relationship;
        private String dateOfBirth;
        private String gender;
        private String preExistingConditions;
    }

    /**
     * Response containing AI-generated document text and a reference ID
     */
    @Data
    @Builder
    public static class GeneratedDocument {
        private String referenceId;
        private String documentTitle;
        private String documentText;     // The full formatted document body
        private String aiSummary;        // 1-2 sentence summary for display
        private String generatedAt;
    }

    /**
     * Request DTO for claim summary letter generation
     */
    @Data
    public static class ClaimSummaryRequest {
        private String claimantName;
        private String policyTitle;
        private String claimType;
        private Double amount;
        private String hospitalName;
        private String incidentDate;
        private String diagnosis;
        private String description;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a formal, personalized Health Declaration Document
     * from the applicant's form data. Replaces the need to upload a physical file.
     */
    public GeneratedDocument generateHealthDeclaration(HealthDeclarationRequest req) {
        String refId = "HD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        String membersSection = buildMembersSection(req.getMembers());
        String declarationsSection = buildDeclarationsSection(req);

        String prompt = String.format(
            "You are a professional Insurance Document Specialist. " +
            "Generate a formal, legally-worded HEALTH DECLARATION DOCUMENT for a health insurance policy application. " +
            "Use official insurance document formatting with proper sections and professional language. " +
            "This is a sworn declaration that will be reviewed by an underwriter.\n\n" +
            "--- APPLICATION DATA ---\n" +
            "Reference ID: %s\n" +
            "Date: %s\n" +
            "Applicant: %s\n" +
            "Policy Applied: %s (%s)\n" +
            "Coverage Amount: ₹%.0f\n" +
            "Nominee: %s (%s)\n\n" +
            "Insured Members:\n%s\n" +
            "Medical Declarations:\n%s\n" +
            "--- END DATA ---\n\n" +
            "Generate a formal HEALTH DECLARATION document with these sections:\n" +
            "1. DECLARATION HEADER (title, ref ID, date)\n" +
            "2. APPLICANT PARTICULARS\n" +
            "3. INSURED MEMBERS SCHEDULE\n" +
            "4. MEDICAL HISTORY DECLARATION (based on the yes/no declarations)\n" +
            "5. LIFESTYLE DECLARATIONS\n" +
            "6. APPLICANT UNDERTAKING (solemn declaration that info is true)\n" +
            "7. UNDERWRITER NOTES SECTION (placeholder for underwriter remarks)\n\n" +
            "Use formal language. Include the reference ID prominently. Keep it professional and concise (max 400 words). " +
            "Format with clear section headers using dashes (---SECTION NAME---). Plain text only, no markdown symbols.",
            refId, date,
            req.getApplicantName(),
            req.getPolicyTitle(), req.getPolicyType(),
            req.getCoverageAmount() != null ? req.getCoverageAmount() : 0,
            req.getNomineeName(), req.getNomineeRelationship(),
            membersSection, declarationsSection
        );

        log.info("Generating AI health declaration for applicant: {}", req.getApplicantName());

        try {
            String docText = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String summaryPrompt = "Summarize this insurance health declaration document in one professional sentence, " +
                "noting the risk level based on the declarations: " + declarationsSection;

            String summary = chatClient.prompt()
                    .user(summaryPrompt)
                    .call()
                    .content();

            return GeneratedDocument.builder()
                    .referenceId(refId)
                    .documentTitle("IQsure Health Declaration — " + req.getPolicyTitle())
                    .documentText(docText != null ? docText.trim() : buildFallbackDeclaration(req, refId, date))
                    .aiSummary(summary != null ? summary.trim() : "Health declaration generated and ready for underwriting review.")
                    .generatedAt(date)
                    .build();
        } catch (Exception e) {
            log.error("LLM health declaration generation failed: {}", e.getMessage());
            return GeneratedDocument.builder()
                    .referenceId(refId)
                    .documentTitle("IQsure Health Declaration — " + req.getPolicyTitle())
                    .documentText(buildFallbackDeclaration(req, refId, date))
                    .aiSummary("Declaration generated. Awaiting underwriting review.")
                    .generatedAt(date)
                    .build();
        }
    }

    /**
     * Generates a formal Claim Summary Letter from the claim form data.
     * This serves as the primary document submitted with the claim.
     */
    public GeneratedDocument generateClaimSummaryLetter(ClaimSummaryRequest req) {
        String refId = "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        String prompt = String.format(
            "You are a professional Claims Document Specialist for IQsure Health Insurance. " +
            "Generate a formal INSURANCE CLAIM SUBMISSION LETTER that the claimant is submitting. " +
            "This document will be reviewed by the claims adjudication team.\n\n" +
            "--- CLAIM DATA ---\n" +
            "Reference ID: %s\n" +
            "Submission Date: %s\n" +
            "Claimant: %s\n" +
            "Linked Policy: %s\n" +
            "Claim Category: %s\n" +
            "Amount Claimed: ₹%.0f\n" +
            "Hospital/Facility: %s\n" +
            "Incident Date: %s\n" +
            "Medical Diagnosis: %s\n" +
            "Incident Description: %s\n" +
            "--- END DATA ---\n\n" +
            "Generate a formal CLAIM SUBMISSION LETTER with these sections:\n" +
            "1. CLAIM REFERENCE HEADER\n" +
            "2. CLAIMANT STATEMENT (first person, formal)\n" +
            "3. INCIDENT CHRONOLOGY (dated timeline of events)\n" +
            "4. MEDICAL PARTICULARS\n" +
            "5. AMOUNT JUSTIFICATION\n" +
            "6. DECLARATION OF TRUTH\n" +
            "7. OFFICER REVIEW NOTES (blank placeholder)\n\n" +
            "Tone: formal, factual, professional. Plain text only (no markdown). Max 400 words. " +
            "Include reference ID prominently. Use ---SECTION--- headers.",
            refId, date,
            req.getClaimantName(),
            req.getPolicyTitle(),
            req.getClaimType(),
            req.getAmount() != null ? req.getAmount() : 0,
            req.getHospitalName() != null ? req.getHospitalName() : "Not specified",
            req.getIncidentDate() != null ? req.getIncidentDate() : "Not specified",
            req.getDiagnosis() != null ? req.getDiagnosis() : "Not specified",
            req.getDescription() != null ? req.getDescription() : "See attached documentation."
        );

        log.info("Generating AI claim letter for: {}", req.getClaimantName());

        try {
            String docText = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return GeneratedDocument.builder()
                    .referenceId(refId)
                    .documentTitle("IQsure Claim Submission — " + req.getPolicyTitle())
                    .documentText(docText != null ? docText.trim() : buildFallbackClaimLetter(req, refId, date))
                    .aiSummary("Formal claim submission letter generated for " + req.getClaimType() + " claim of ₹" +
                        String.format("%.0f", req.getAmount() != null ? req.getAmount() : 0) + ". Ready for adjudication.")
                    .generatedAt(date)
                    .build();
        } catch (Exception e) {
            log.error("LLM claim letter generation failed: {}", e.getMessage());
            return GeneratedDocument.builder()
                    .referenceId(refId)
                    .documentTitle("IQsure Claim Submission — " + req.getPolicyTitle())
                    .documentText(buildFallbackClaimLetter(req, refId, date))
                    .aiSummary("Claim letter generated and ready for submission.")
                    .generatedAt(date)
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String buildMembersSection(List<MemberSummary> members) {
        if (members == null || members.isEmpty()) return "Primary insured only.";
        StringBuilder sb = new StringBuilder();
        for (MemberSummary m : members) {
            sb.append(String.format("  - %s (%s), DOB: %s, Gender: %s, Pre-existing: %s\n",
                m.getFullName(), m.getRelationship(), m.getDateOfBirth(),
                m.getGender(), m.getPreExistingConditions() != null ? m.getPreExistingConditions() : "None"));
        }
        return sb.toString();
    }

    private String buildDeclarationsSection(HealthDeclarationRequest req) {
        return String.format(
            "  - Hospitalized in last 12 months: %s\n" +
            "  - Chronic illness history (Diabetes/Heart/Cancer): %s\n" +
            "  - Smoking or alcohol consumption: %s\n" +
            "  - Surgical history (last 5 years): %s",
            Boolean.TRUE.equals(req.getHospitalizedLastYear()) ? "YES" : "NO",
            Boolean.TRUE.equals(req.getChronicConditions()) ? "YES" : "NO",
            Boolean.TRUE.equals(req.getSmokeOrAlcohol()) ? "YES" : "NO",
            Boolean.TRUE.equals(req.getSurgicalHistory()) ? "YES" : "NO"
        );
    }

    private String buildFallbackDeclaration(HealthDeclarationRequest req, String refId, String date) {
        return String.format(
            "---IQSURE HEALTH DECLARATION---\n\n" +
            "Reference ID: %s\nDate: %s\n\n" +
            "---APPLICANT PARTICULARS---\n" +
            "Name: %s\nPolicy Applied: %s\nNominee: %s (%s)\n\n" +
            "---INSURED MEMBERS---\n%s\n" +
            "---MEDICAL DECLARATIONS---\n%s\n\n" +
            "---APPLICANT UNDERTAKING---\n" +
            "I, %s, hereby solemnly declare that all information provided above is accurate " +
            "and complete to the best of my knowledge. I understand that any misrepresentation " +
            "may result in rejection of claims or cancellation of the policy.\n\n" +
            "---UNDERWRITER REVIEW NOTES---\n[Reserved for underwriter use]",
            refId, date,
            req.getApplicantName(), req.getPolicyTitle(),
            req.getNomineeName(), req.getNomineeRelationship(),
            buildMembersSection(req.getMembers()),
            buildDeclarationsSection(req),
            req.getApplicantName()
        );
    }

    private String buildFallbackClaimLetter(ClaimSummaryRequest req, String refId, String date) {
        return String.format(
            "---IQSURE CLAIM SUBMISSION LETTER---\n\n" +
            "Reference ID: %s\nDate: %s\n\n" +
            "---CLAIMANT STATEMENT---\n" +
            "I, %s, hereby submit this claim under policy '%s' for a %s claim totaling ₹%.0f.\n\n" +
            "---INCIDENT PARTICULARS---\n" +
            "Incident Date: %s\nHospital: %s\nDiagnosis: %s\n\n" +
            "---DESCRIPTION---\n%s\n\n" +
            "---DECLARATION OF TRUTH---\n" +
            "I declare that all information herein is true and accurate.\n\n" +
            "---OFFICER REVIEW NOTES---\n[Reserved for claims officer use]",
            refId, date,
            req.getClaimantName(), req.getPolicyTitle(), req.getClaimType(),
            req.getAmount() != null ? req.getAmount() : 0,
            req.getIncidentDate(), req.getHospitalName(), req.getDiagnosis(),
            req.getDescription() != null ? req.getDescription() : "N/A"
        );
    }
}
