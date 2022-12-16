package com.lol.match.main.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.HashOperations;
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

    // // TODO : 삭제 구현이 필요함, 좀 더 생각해봐야하는 부분
    // @GetMapping("/match")
    // @ResponseBody
    // public String match(@RequestBody UserMatchDto userMatchDto) {

    // int mmr = userMatchDto.getMmr();
    // int min = mmr > 150 ? mmr - 50 : 100;
    // int max = mmr + 50;

    // String listname = "bronze_100_167";

    // // String listname = "queue";

    // int time = 1;
    // GroupMatchDto groupMatchDto = new GroupMatchDto(listname, max, min, time);

    // redisTemplate.opsForList().leftPush(listname, userMatchDto);
    // redisTemplate.opsForList().leftPush("queue", groupMatchDto);

    // // redisTemplate.opsForList().leftPush(listname+1, "");
    // // redisTemplate.opsForList().leftPush(listname+2, "");
    // // redisTemplate.opsForList().leftPush(listname+3, "");
    // // redisTemplate.opsForList().leftPush(listname+4, "");
    // // redisTemplate.opsForList().leftPush(listname+5, "");

    // RedisOperations<String, Object> operations =
    // redisTemplate.opsForList().getOperations();
    // operations.opsForList().remove(listname, time, operations);
    // redisTemplate.setValueSerializer(new StringRedisSerializer());

    // List<Object> a = operations.opsForList().range(listname, 0, 9);

    // // 리스트는 있고, 11명이 되려고 할때 1초 쓰레드 추가
    // try {

    // for (int i = 0; i < a.size(); i++) {
    // UserMatchDto userMatchDto2 = objectMapper.readValue(a.get(i).toString(),
    // UserMatchDto.class);
    // System.out.println("값 나와랏! : "+userMatchDto2.toString());
    // }

    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // // System.out.println(operations.opsForList().range("chatNumber" + idx, 0,
    // -1)); // Redis Data List 출력
    // return "OK";
    // }

    // TODO : 삭제 구현이 필요함, 좀 더 생각해봐야하는 부분
    @GetMapping("/match")
    @ResponseBody
    public String match(@RequestBody UserMatchDto userMatchDto) {

        int mmr = userMatchDto.getMmr();

        // TODO : 해쉬맵이 있는지 체크하는 메소드 추가 필요
        String listName = isMap(mmr, userMatchDto.getRank());

        // 맵이 없어서 새롭게 생성하는 경우
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Map<String, Object> map = new HashMap<>();

        map.put("firstName", "Gyunny");
        map.put("lastName", "Choi");
        map.put("gender", "Man");
        hashOperations.putAll("key", map);

        String firstName = (String) redisTemplate.opsForHash().get("key", "firstName");
        String lastName = (String) redisTemplate.opsForHash().get("key", "lastName");
        String gender = (String) redisTemplate.opsForHash().get("key", "gender");
        System.out.println(firstName);
        System.out.println(lastName);
        System.out.println(gender);

        // String listname = "queue";

        int time = 1;
        // GroupMatchDto groupMatchDto = new GroupMatchDto(listname, max, min, time);

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        operations.opsForList().remove("QueueList", time, operations);
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        List<Object> a = operations.opsForList().range("키이름", 0, operations.opsForList().size("QueueList")-1);

        // 리스트는 있고, 11명이 되려고 할때 1초 쓰레드 추가
        try {

            for (int i = 0; i < a.size(); i++) {
                UserMatchDto userMatchDto2 = objectMapper.readValue(a.get(i).toString(), UserMatchDto.class);
                System.out.println("값 나와랏! : " + userMatchDto2.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.out.println(operations.opsForList().range("chatNumber" + idx, 0, -1));
        // // Redis Data List 출력
        return "OK";
    }

    @GetMapping("/write")
    @ResponseBody
    public String write(@RequestBody UserMatchDto userMatchDto) {

        int mmr = userMatchDto.getMmr();
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        String listname = userMatchDto.getRank() + "_" + min + "_" + max;

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
                System.out.println("값 나와랏! : " + userMatchDto2.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.out.println(operations.opsForList().range("chatNumber" + idx, 0, -1));
        // // Redis Data List 출력
        return "OK";
    }

    @GetMapping("/key")
    @ResponseBody
    public String key() {
        isMap(0, "");

        return "ok";
    }

    // 맵이 있는지 확인
    private String isMap(int mmr, String rank) {
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        String queueName = "";
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        List<String> list = new ArrayList<>();

        if (rank.equals("Iron") || rank.equals("Bronze")) {
            list.add("map:Iron");
            list.add("map:Bronze");
            list.add("map:Silver");
        } else if (rank.equals("Silver")) {
            list.add("map:Iron");
            list.add("map:Bronze");
            list.add("map:Silver");
            list.add("map:Gold");
        } else if (rank.equals("Gold")) {
            list.add("map:Silver");
            list.add("map:Gold");
            list.add("map:Platinum");
        } else if (rank.equals("Platinum")) {
            list.add("map:Gold");
            list.add("map:Platinum");
            list.add("map:Emerald");
        } else if (rank.equals("Emerald")) {
            list.add("map:Platinum");
            list.add("map:Emerald");
            list.add("map:Diamond");
        } else if (rank.equals("Diamond")) {
            list.add("map:Emerald");
            list.add("map:Diamond");
            list.add("map:Master");
        } else if (rank.equals("Master")) {
            list.add("map:Diamond");
            list.add("map:Master");
            list.add("map:GrandMaster");
        } else if (rank.equals("GrandMaster")) {
            list.add("map:Master");
            list.add("map:GrandMaster");
        } else if (rank.equals("Challenger")) {
            list.add("map:Challenger");
        }
        for (int i = 0; i < list.size(); i++) {

            List<Object> queueList = operations.opsForList().range("키이름", 0, operations.opsForList().size("QueueList")-1);

            for (Object key : queueList) {
                String[] name = String.valueOf(key).split("_");
                System.out.println("min값 : " + name[1]);
                System.out.println("max값 : " + name[2]);
                if (Integer.parseInt(name[1]) <= min) {
                    if (Integer.parseInt(name[2]) >= max) {
                        System.out.println("범위 안에 잘 들어옴 큐 이름을 반환" + key);
                        queueName = (String)key;
                        return queueName;
                    }
                }
            }               
        }

        queueName = rank+"_"+min+"_"+max;
        return queueName;
    }

}
