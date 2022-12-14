package com.lol.match.main.controller;

import java.util.List;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.match.domain.dto.GroupMatchDto;
import com.lol.match.domain.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {
    
    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    // TODO : 삭제 구현이 필요함, 좀 더 생각해봐야하는 부분
    @GetMapping("/match")
    @ResponseBody
    public String match(@RequestBody UserMatchDto userMatchDto) {
        
        int mmr = userMatchDto.getMmr();
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        String listname = "bronze_100_167";
        
        // String listname = "queue";

        int time = 1;
        GroupMatchDto groupMatchDto = new GroupMatchDto(listname, max, min, time);

        redisTemplate.opsForList().leftPush(listname, userMatchDto);
        redisTemplate.opsForList().leftPush("queue", groupMatchDto);

        // redisTemplate.opsForList().leftPush(listname+1, "");
        // redisTemplate.opsForList().leftPush(listname+2, "");
        // redisTemplate.opsForList().leftPush(listname+3, "");
        // redisTemplate.opsForList().leftPush(listname+4, "");
        // redisTemplate.opsForList().leftPush(listname+5, "");

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        operations.opsForList().remove(listname, time, operations);
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        List<Object> a = operations.opsForList().range(listname, 0, 9);

        // 리스트는 있고, 11명이 되려고 할때 1초 쓰레드 추가
        try {

            for (int i = 0; i < a.size(); i++) {
                UserMatchDto userMatchDto2 = objectMapper.readValue(a.get(i).toString(), UserMatchDto.class);                
                System.out.println("값 나와랏! : "+userMatchDto2.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.out.println(operations.opsForList().range("chatNumber" + idx, 0, -1));  // Redis Data List 출력
        return "OK";
    }

    @GetMapping("/write")
    @ResponseBody
    public String write(@RequestBody UserMatchDto userMatchDto) {
        
        int mmr = userMatchDto.getMmr();
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        String listname = userMatchDto.getRank() + "_" + min + "_"+max;
        
        // String listname = "queue";

        int time = 1;
        GroupMatchDto groupMatchDto = new GroupMatchDto(listname, max, min, time);

        redisTemplate.opsForList().leftPush(listname, userMatchDto);
        redisTemplate.opsForList().leftPush("queue", groupMatchDto);

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        List<Object> a = operations.opsForList().range(listname, 0, 9);

        

        // 리스트는 있고, 11명이 되려고 할때 1초 쓰레드 추가
        try {

            for (int i = 0; i < a.size(); i++) {
                UserMatchDto userMatchDto2 = objectMapper.readValue(a.get(0).toString(), UserMatchDto.class);                
                System.out.println("값 나와랏! : "+userMatchDto2.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.out.println(operations.opsForList().range("chatNumber" + idx, 0, -1));  // Redis Data List 출력
        return "OK";
    }


}
