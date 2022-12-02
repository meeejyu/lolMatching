package com.lol.matching.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
// import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

// import com.amazonaws.services.s3.internal.eventstreaming.Message;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
// import com.amazonaws.services.sqs.model.Message;

@Service
public class MainService {
    
    private final QueueMessagingTemplate queueMessagingTemplate;

    @Autowired
    public MainService(AmazonSQS amazonSQS) {
        this.queueMessagingTemplate = new QueueMessagingTemplate((AmazonSQSAsync) amazonSQS);
    }

    public void sendMessage(org.springframework.messaging.Message<?> message) {
        // Message<?> newMessage = MessageBuilder.withPayload(message).build();
        Message<?> newMessage = MessageBuilder.withPayload(message).build();
        queueMessagingTemplate.send("보낼 메세지", message);
    }
}
