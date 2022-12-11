package com.lol.matching.aws;

import java.util.List;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
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
        
        ObjectMapper objectMapper = new ObjectMapper();

        UserMatchDto userMatchDto;
        try {

            System.out.println(messages.get(0).getBody());
            String test = messages.get(0).getBody();
            // String test2 = messages.get(1).getBody();
            userMatchDto = objectMapper.readValue(test, UserMatchDto.class);

            System.out.println(userMatchDto.getPosition());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
