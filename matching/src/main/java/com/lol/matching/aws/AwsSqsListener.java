package com.lol.matching.aws;

import java.util.Map;

import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.lol.matching.dto.GroupMatchDto;
import com.lol.matching.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsSqsListener {


	// 유저 정보 저장
	// @SqsListener(value = "${cloud.aws.sqs.queue.individual.name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
	public void UserListen(@Payload UserMatchDto info, @Headers Map<String, String> headers, Acknowledgment ack) {
		log.info("-------------------------------------start User SqsListener");
		log.info("-------------------------------------info {}", info);
		// log.info("-------------------------------------headers {}", headers);
        //수신후 삭제처리
		// TODO : 삭제 조건 추가
		
		// ack.acknowledge();
	}

	// 그룹 정보 저장
	// @SqsListener(value = "${cloud.aws.sqs.queue.group.name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
	public void GroupListen(@Payload GroupMatchDto info, @Headers Map<String, String> headers, Acknowledgment ack) {
		log.info("-------------------------------------start Group SqsListener");
		log.info("-------------------------------------info {}", info);
		// log.info("-------------------------------------headers {}", headers);
        //수신후 삭제처리
		// TODO : 삭제 조건 추가
		
		// ack.acknowledge();
	}
	
	// test
	// @SqsListener(value = "${cloud.aws.sqs.queue.group.name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
	// public void listen(@Payload String info, @Headers Map<String, String> headers, Acknowledgment ack) {
	// 	log.info("-------------------------------------start SqsListener");
	// 	log.info("-------------------------------------info {}", info);
	// 	log.info("-------------------------------------headers {}", headers);
    //     //수신후 삭제처리
	// 	// TODO : 삭제 조건 추가
		
	// 	// ack.acknowledge();
	// }

}
