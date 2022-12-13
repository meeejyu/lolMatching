package com.lol.matching.aws;

import java.util.List;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.matching.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AwsSqsRead {

    private final AmazonSQS sqs;
    
    public void readMessage(String queueName) {
        String queue_url = sqs.getQueueUrl(queueName).getQueueUrl();
        List<Message> messages = sqs.receiveMessage(queue_url).getMessages();

        // ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueName);
            // receiveMessageRequest.setMaxNumberOfMessages(10);
            // receiveMessageRequest.withMaxNumberOfMessages(10);
            // receiveMessageRequest.withWaitTimeSeconds(10);
            // receiveMessageRequest.withMaxNumberOfMessages(10);
            // receiveMessageRequest.withMaxNumberOfMessages(10).withWaitTimeSeconds(10);
        // List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
        // sqs.receiveMessage("all")
        // sqs.receiveMessage(queue_url).getMessages()
        // messages.size();
        for(Message message : messages) {
            System.out.println("message : " + message.getBody());
        }
        System.out.println("메세지 사이즈"+messages.size());
        // ObjectMapper objectMapper = new ObjectMapper();

        // UserMatchDto userMatchDto;
        // try {
        //     System.out.println("메세지 읽기 -----------");
        //     for (int i = 0; i < messages.size(); i++) {
        //         String test = messages.get(i).getBody();
        //         System.out.println(test);
        //         userMatchDto = objectMapper.readValue(test, UserMatchDto.class);
    
        //         System.out.println(userMatchDto.getPosition());   
        //     }
        // } catch (JsonProcessingException e) {
        //     e.printStackTrace();
        // }
    }
}
