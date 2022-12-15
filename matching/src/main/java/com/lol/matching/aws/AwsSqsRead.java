package com.lol.matching.aws;

import java.util.List;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
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
        
        // AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
        String queue_url = sqs.getQueueUrl(queueName).getQueueUrl();

        // List<Message> messages = sqs.receiveMessage(queue_url).getMessages();
        // sqs.shutdown();

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueName);
            receiveMessageRequest.setVisibilityTimeout(10); //폴링 시간
            receiveMessageRequest.withMaxNumberOfMessages(10); // 폴링 메세지
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
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
