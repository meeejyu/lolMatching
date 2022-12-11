package com.lol.matching.aws;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.lol.matching.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AwsSqsCreate {

    private final AmazonSQS sqs;
    
    public String createQueue(UserMatchDto userMatchDto) {

        int mmr = userMatchDto.getMmr();
        int min = mmr - 50 >= 100? mmr-50 : mmr;  
        int max = mmr + 50;

        String queueName = userMatchDto.getRank()+"_"+min+"_"+max;

        // 대기열 검토
        getQueues();

        CreateQueueRequest create_request = new CreateQueueRequest(queueName)
        .addAttributesEntry("DelaySeconds", "1") // 전송 지연
        .addAttributesEntry("MessageRetentionPeriod", "86400") // 메세지 보존 기간
        .addAttributesEntry("ReceiveMessageWaitTimeSeconds", "3") // 메세지 수신 대기시간
        .addAttributesEntry("KmsMasterKeyId", ""); // 암호 비활성화
        
        try {
            sqs.createQueue(create_request);
            // String queue_url = sqs.getQueueUrl(queueName).getQueueUrl();
            return queueName;
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            }
            return "";
        }
    }

    public void getQueues() {

        ListQueuesResult queuesResult = sqs.listQueues();
        for (String url : queuesResult.getQueueUrls()) {
            String[] a = url.split("/"); 
            System.out.println("내용 " + a[a.length-1]);
            System.out.println(url);
            String[] b = a[a.length-1].split("_");
            System.out.println("min값 : "+b[1]);
            System.out.println("max값 : "+b[2]);

        }
    }
    
}
