package com.bbn.marti.email;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

import com.bbn.marti.config.Email;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;


public class EmailClient {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EmailClient.class);

    public static void sendEmail(Email config, String subject, String text, String to, String cc,
                                 HashMap<String, byte[]> attachments) {
        try {

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(config.getHost());
            mailSender.setPort(config.getPort());
            mailSender.setUsername(config.getUsername());
            mailSender.setPassword(config.getPassword());

            MimeMessage message = mailSender.createMimeMessage();
            message.setContent(text, "text/html");

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setSubject(subject);
            helper.setText(text);
            helper.setFrom(config.getFrom());
            helper.setTo(to);

            if (cc != null) {
                helper.addCc(cc);
            }

            if (attachments != null) {
                for (Map.Entry<String, byte[]> attachment : attachments.entrySet()) {
                    helper.addAttachment(attachment.getKey(), new ByteArrayResource(attachment.getValue()));
                }
            }

            mailSender.send(message);

        } catch (Exception e) {
            logger.error("exception in sendEmail!", e);
        }
    }
}