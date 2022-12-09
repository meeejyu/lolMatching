package com.lol.matching.aws;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AwsSqsCreate {

    private final AmazonSQS sqs;
    
    public void createQueue() {

        CreateQueueRequest create_request = new CreateQueueRequest("test3")
        .addAttributesEntry("DelaySeconds", "1") // 전송 지연
        .addAttributesEntry("MessageRetentionPeriod", "86400") // 메세지 보존 기간
        .addAttributesEntry("ReceiveMessageWaitTimeSeconds", "3") // 메세지 수신 대기시간
        .addAttributesEntry("", ""); 
        
        try {
            sqs.createQueue(create_request);
            // String queue_url = sqs.getQueueUrl("test2").getQueueUrl();

        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            }
        }
    }
    
}
