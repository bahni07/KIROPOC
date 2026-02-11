package com.example.userregistration.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send verification email with token link
     * 
     * @param toEmail recipient email address
     * @param firstName recipient first name
     * @param verificationToken the verification token (plain text, not hashed)
     * @throws MessagingException if email sending fails
     */
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken) 
            throws MessagingException {
        String subject = "Verify Your Email Address";
        String verificationLink = baseUrl + "/api/v1/register/verify?token=" + verificationToken;
        
        String htmlContent = buildVerificationEmailHtml(firstName, verificationLink);
        String plainTextContent = buildVerificationEmailPlainText(firstName, verificationLink);
        
        sendEmail(toEmail, subject, htmlContent, plainTextContent);
    }

    /**
     * Send welcome email after successful verification
     * 
     * @param toEmail recipient email address
     * @param firstName recipient first name
     * @throws MessagingException if email sending fails
     */
    public void sendWelcomeEmail(String toEmail, String firstName) throws MessagingException {
        String subject = "Welcome to Our Service!";
        
        String htmlContent = buildWelcomeEmailHtml(firstName);
        String plainTextContent = buildWelcomeEmailPlainText(firstName);
        
        sendEmail(toEmail, subject, htmlContent, plainTextContent);
    }

    /**
     * Send email with both HTML and plain text content
     */
    private void sendEmail(String toEmail, String subject, String htmlContent, String plainTextContent) 
            throws MessagingException {
        if (!emailEnabled) {
            logger.info("Email sending disabled. Would send email to: {}, Subject: {}", toEmail, subject);
            logger.debug("Email content (plain text): {}", plainTextContent);
            return;
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(plainTextContent, htmlContent);
        
        mailSender.send(message);
    }

    /**
     * Build HTML content for verification email
     */
    private String buildVerificationEmailHtml(String firstName, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; 
                              color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Email Verification</h1>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>Thank you for registering! Please verify your email address by clicking the button below:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verify Email Address</a>
                        </p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #666;">%s</p>
                        <p>This link will expire in 24 hours.</p>
                        <p>If you didn't create an account, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 User Registration Service. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, verificationLink, verificationLink);
    }

    /**
     * Build plain text content for verification email
     */
    private String buildVerificationEmailPlainText(String firstName, String verificationLink) {
        return """
            Hi %s,
            
            Thank you for registering! Please verify your email address by clicking the link below:
            
            %s
            
            This link will expire in 24 hours.
            
            If you didn't create an account, please ignore this email.
            
            ---
            User Registration Service
            """.formatted(firstName, verificationLink);
    }

    /**
     * Build HTML content for welcome email
     */
    private String buildWelcomeEmailHtml(String firstName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome!</h1>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>Welcome to our service! Your email has been successfully verified.</p>
                        <p>You can now access all features of your account.</p>
                        <p>If you have any questions, feel free to contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 User Registration Service. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName);
    }

    /**
     * Build plain text content for welcome email
     */
    private String buildWelcomeEmailPlainText(String firstName) {
        return """
            Hi %s,
            
            Welcome to our service! Your email has been successfully verified.
            
            You can now access all features of your account.
            
            If you have any questions, feel free to contact our support team.
            
            ---
            User Registration Service
            """.formatted(firstName);
    }
}
