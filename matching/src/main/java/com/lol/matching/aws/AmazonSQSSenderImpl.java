package com.lol.matching.aws;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.lol.matching.dto.EcmDto;

public interface AmazonSQSSenderImpl {
    SendMessageResult sendMessage(EcmDto message) throws JsonProcessingException;
}
