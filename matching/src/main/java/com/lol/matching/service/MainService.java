package com.lol.matching.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.lol.matching.dto.GroupMatchDto;
import com.lol.matching.dto.UserMatchDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MainService {
    
    @Value("${cloud.aws.sqs.queue.group.name}")
    private String groupName;

    @Value("${cloud.aws.sqs.queue.individual.name}")
    private String userName;

    private final QueueMessagingTemplate queueMessagingTemplate;

    @Autowired
    public MainService(AmazonSQS amazonSQS) {
        this.queueMessagingTemplate = new QueueMessagingTemplate((AmazonSQSAsync) amazonSQS);
    }

    // 유저 정보 가져오기
    public void getUserMessage() {
        UserMatchDto userMatchDto = queueMessagingTemplate.receiveAndConvert(userName, UserMatchDto.class);
        System.out.println("SQS로부터 받은 메시지 : " + userMatchDto);
    }

    public void getGroupMessage() {
        GroupMatchDto groupMatchDto = queueMessagingTemplate.receiveAndConvert(groupName, GroupMatchDto.class);
        System.out.println("SQS로부터 받은 메시지 : " + groupName);
    }

    public void sendMessage(UserMatchDto userMatchDto, String queueName) {
        log.info("SQS에 전달합니다 : ");
        queueMessagingTemplate.convertAndSend(queueName, userMatchDto);
    }

}
