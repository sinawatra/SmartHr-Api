package com.smarthr.smarthr.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendAnnouncementNotification(String chatId, String title, String content) {
        if (chatId == null || chatId.isBlank()) {
            log.warn("No Telegram chatId provided for announcement. Skipping push.");
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        String messageText = String.format("🚨 *IMPORTANT ANNOUNCEMENT* 🚨\n\n*%s*\n\n%s", title, content);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", messageText);
        requestBody.put("parse_mode", "Markdown");

        try {
            restTemplate.postForEntity(url, requestBody, String.class);
            log.info("Telegram notification sent to channel: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to {}: {}", chatId, e.getMessage());
        }
    }
}