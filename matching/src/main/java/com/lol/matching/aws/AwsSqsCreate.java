package com.lol.matching.aws;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.lol.matching.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AwsSqsCreate {

    private final AmazonSQS sqs;

    public String createQueue(UserMatchDto userMatchDto) {

        int mmr = userMatchDto.getMmr();
        int min = mmr - 50 >= 100 ? mmr - 50 : mmr;
        int max = mmr + 50;

        String queueName = userMatchDto.getRank() + "_" + min + "_" + max;

        // 대기열 검토
        getQueues(userMatchDto);

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

    public void getQueues(UserMatchDto userMatchDto) {

        ListQueuesResult queuesResult = sqs.listQueues();
        for (String url : queuesResult.getQueueUrls()) {
            String[] a = url.split("/");
            System.out.println("내용 " + a[a.length - 1]);
            System.out.println(url);
            if (a[a.length - 1].contains("_")) {
                String[] b = a[a.length - 1].split("_");
                System.out.println("min값 : " + b[1]);
                System.out.println("max값 : " + b[2]);
                if (Integer.parseInt(b[1]) <= userMatchDto.getMmr()) {
                    if (Integer.parseInt(b[2]) >= userMatchDto.getMmr()) {
                        System.out.println(
                                "할룽 범위 안에 잘 들어왓네 큐 이름 : " + a[a.length - 1] + " 유저 mmr : " + userMatchDto.getMmr());
                    }
                }
            }
        }

    }

    public void getQueueInfo() {

        String attr = "ApproximateNumberOfMessages";
        Map<String, String> attributes = sqs.getQueueAttributes(new GetQueueAttributesRequest("https://sqs.ap-northeast-2.amazonaws.com/843354017769/bronze_150_250").withAttributeNames(attr)).getAttributes();
        int count = Integer.parseInt(attributes.get(attr));
        System.out.println("대기열 사이즈,, 젭알 나와라 : " + count);
    }

    public void deleteQueue() {

        sqs.deleteQueue("https://sqs.ap-northeast-2.amazonaws.com/843354017769/gold_650_750");
    }

   

}
