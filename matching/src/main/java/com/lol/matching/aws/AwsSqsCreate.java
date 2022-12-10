package com.lol.matching.aws;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.lol.matching.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AwsSqsCreate {

    private final AmazonSQS sqs;
    
    public void createQueue(UserMatchDto userMatchDto) {

        int mmr = userMatchDto.getMmr();
        int min = mmr - 50 >= 100? mmr-50 : mmr;  
        int max = mmr + 50;

        String queueName = userMatchDto.getRank()+"_"+min+"_"+max;
        CreateQueueRequest create_request = new CreateQueueRequest(queueName)
        .addAttributesEntry("DelaySeconds", "1") // 전송 지연
        .addAttributesEntry("MessageRetentionPeriod", "86400") // 메세지 보존 기간
        .addAttributesEntry("ReceiveMessageWaitTimeSeconds", "3") // 메세지 수신 대기시간
        .addAttributesEntry("KmsMasterKeyId", ""); // 암호 비활성화
        
        try {
            sqs.createQueue(create_request);
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            }
        }
    }
    
}
