package org.hartford.iqsure.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@iqsure.com}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset OTP — IQsure");
            helper.setText(buildOtpHtml(otp), true);
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendGenericEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("Email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpHtml(String otp) {
        return """
            <div style="font-family: 'Outfit', 'Inter', 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #ffb6c1; border-radius: 24px; overflow: hidden; box-shadow: 0 15px 35px rgba(233, 30, 99, 0.1);">
              <!-- Header with Brand Gradient -->
              <div style="background: linear-gradient(135deg, #8B003F 0%%, #E91E63 100%%); padding: 48px 32px; text-align: center;">
                <h1 style="color: white; margin: 0; font-size: 34px; letter-spacing: 1px; font-weight: 800;">IQsure</h1>
                <p style="color: #ffd1dc; margin: 10px 0 0; font-size: 16px; font-weight: 500;">Secure Your Future with Knowledge</p>
              </div>
              
              <div style="padding: 40px; text-align: center; background-color: #ffffff;">
                <h2 style="color: #4a154b; margin-bottom: 16px; font-size: 24px; font-weight: 700;">Password Reset Request</h2>
                <p style="font-size: 16px; color: #8a4b6b; line-height: 1.6; margin-bottom: 32px;">No worries! Use the following dynamic OTP to regain access to your account:</p>
                
                <!-- Premium OTP Display -->
                <div style="font-size: 48px; font-weight: 800; letter-spacing: 12px; color: #e91e63; margin: 20px 0; padding: 30px; background: #fff0f5; border-radius: 16px; display: inline-block; border: 2px dashed #e91e63;">
                  %s
                </div>
                
                <p style="color: #64748b; font-size: 14px; margin-top: 32px; font-family: 'Inter', sans-serif;">
                  This OTP is valid for <strong>10 minutes</strong>.<br>
                  If you did not request this, please secure your account immediately.
                </p>
              </div>
              
              <!-- Footer -->
              <div style="background: #fff8f8; padding: 24px; text-align: center; font-size: 13px; color: #8a4b6b; border-top: 1px solid #fce4ec;">
                © 2026 IQsure Gamified Insurance Platform • Learn Smarter, Save Better.
              </div>
            </div>
            """.formatted(otp);
    }
}
