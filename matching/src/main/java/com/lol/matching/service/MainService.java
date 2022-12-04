package com.lol.matching.service;

import org.springframework.stereotype.Service;

@Service
public class MainService {
    
    // private final QueueMessagingTemplate queueMessagingTemplate;

    // @Autowired
    // public MainService(AmazonSQS amazonSQS) {
    //     this.queueMessagingTemplate = new QueueMessagingTemplate((AmazonSQSAsync) amazonSQS);
    // }

    // public void sendMessage(org.springframework.messaging.Message<?> message) {
    //     // Message<?> newMessage = MessageBuilder.withPayload(message).build();
    //     Message<?> newMessage = MessageBuilder.withPayload(message).build();
    //     queueMessagingTemplate.send("보낼 메세지", message);
    // }
}
