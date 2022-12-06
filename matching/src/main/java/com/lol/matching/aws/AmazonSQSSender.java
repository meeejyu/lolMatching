package com.lol.matching.aws;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.matching.dto.EcmDto;
import com.lol.matching.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class AmazonSQSSender implements AmazonSQSSenderImpl {
    
    @Value("${cloud.aws.sqs.queue.group.url}")
    private String url;

    // @Value("${cloud.aws.sqs.queue.individual.url}")
    // private String userUrl;

    private final ObjectMapper objectMapper;
    private final AmazonSQS amazonSQS;

    @Override
    public SendMessageResult sendMessage(EcmDto msg) throws JsonProcessingException {
        SendMessageRequest sendMessageRequest = new SendMessageRequest(url,
                objectMapper.writeValueAsString(msg))
                .withMessageGroupId("sqs-test")
                .withMessageDeduplicationId(UUID.randomUUID().toString());
        return amazonSQS.sendMessage(sendMessageRequest);
    }

    // @Override
    // public SendMessageResult sendUserMatchMessage(UserMatchDto message) throws JsonProcessingException {
    //     SendMessageRequest sendMessageRequest = new SendMessageRequest();

    //     return null;
    // }

    // 메세지 가져오는 메소드
	// public void test() {

	// 	System.out.println("---------메세지 확인------");
	// 	List<Message> messages = amazonSQS.receiveMessage(url).getMessages();
    //     System.out.println("리스트 사이즈 : " + messages.size());
    //     System.out.println("메세지 확인 : "+messages.get(0));
    //     for (int i = 0; i < messages.size(); i++) {
    //         System.out.println(i+" 번째 "+"값 : "+messages.get(i).getBody());
    //     }
    //     System.out.println(messages.toString());

	// }

}