package com.example.ATSCIRCLE.Service.Sales;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendNotification(String userId, String message) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, message);
        System.out.println("🔔 Notification sent to user: " + userId);
    }
}
