package com.lol.matching.aws;

import java.util.List;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;

public class AwsSqsRead {
    
    String a = "";

    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    public void test() {
        // List<Message> messages = sqs.receiveMessage(a).getMessages();
    }
}
