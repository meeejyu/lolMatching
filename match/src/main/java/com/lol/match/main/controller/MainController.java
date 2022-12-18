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

        // TODO : 해쉬맵이 있는지 체크하는 메소드 추가 필요, 맵값으로 삭제 확인해서 하기
        String listName = isMap(mmr, userMatchDto.getRank());

        System.out.println("listName : " + listName);

        // 해당 큐가 있는지 확인 
        Set<String> keys = redisTemplate.keys("map:"+listName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        if(keys.size() > 0) {
            // 맵에 유저 정보 넣어주기
            hashOperations.put("map:"+listName, userMatchDto.getUserId(), userMatchDto);
        }
        else {
            // 맵을 새롭게 만들어준다
            Map<String, Object> map = new HashMap<>();
            hashOperations.put("map:"+listName, userMatchDto.getUserId(), userMatchDto);
        }
        redisTemplate.opsForList().rightPush("queueList", listName);


        // 맵이 없어서 새롭게 생성하는 경우
        // HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        // Map<String, Object> map = new HashMap<>();

        // map.put("firstName", "Gyunny");
        // map.put("lastName", "Choi");
        // map.put("gender", "Man");
        // hashOperations.putAll("key", map);

        // String firstName = (String) redisTemplate.opsForHash().get("key", "firstName");
        // String lastName = (String) redisTemplate.opsForHash().get("key", "lastName");
        // String gender = (String) redisTemplate.opsForHash().get("key", "gender");
        // System.out.println(firstName);
        // System.out.println(lastName);
        // System.out.println(gender);

        // // String listname = "queue";

        // int time = 1;
        // // GroupMatchDto groupMatchDto = new GroupMatchDto(listname, max, min, time);

        // RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        // operations.opsForList().remove("QueueList", time, operations);
        // redisTemplate.setValueSerializer(new StringRedisSerializer());

        // List<Object> a = operations.opsForList().range("키이름", 0, operations.opsForList().size("QueueList")-1);

        // // 리스트는 있고, 11명이 되려고 할때 1초 쓰레드 추가
        // try {

        //     for (int i = 0; i < a.size(); i++) {
        //         UserMatchDto userMatchDto2 = objectMapper.readValue(a.get(i).toString(), UserMatchDto.class);
        //         System.out.println("값 나와랏! : " + userMatchDto2.toString());
        //     }

        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

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
        return "OK";
    }

    @GetMapping("/key")
    @ResponseBody
    public String key() {
        isMap(0, "");

        return "ok";
    }

    // 리스트값 들고오나?
    @GetMapping("/keys")
    @ResponseBody
    public String keys() {
        // Set<String> keys = redisTemplate.keys("posts:*");
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        List<Object> queueList = operations.opsForList().range("list_test", 1, -1);
        for(Object a : queueList) {
            System.out.println("값 확인 : "+ a);
        }

        return "ok";
    }

    // 맵이 있는지 확인
    private String isMap(int mmr, String rank) {
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        String queueName = "";
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        List<String> rankList = new ArrayList<>();

        if (rank.equals("Iron") || rank.equals("Bronze")) {
            rankList.add("Iron");
            rankList.add("Bronze");
            rankList.add("Silver");
        } else if (rank.equals("Silver")) {
            rankList.add("Iron");
            rankList.add("Bronze");
            rankList.add("Silver");
            rankList.add("Gold");
        } else if (rank.equals("Gold")) {
            rankList.add("Silver");
            rankList.add("Gold");
            rankList.add("Platinum");
        } else if (rank.equals("Platinum")) {
            rankList.add("Gold");
            rankList.add("Platinum");
            rankList.add("Emerald");
        } else if (rank.equals("Emerald")) {
            rankList.add("Platinum");
            rankList.add("Emerald");
            rankList.add("Diamond");
        } else if (rank.equals("Diamond")) {
            rankList.add("Emerald");
            rankList.add("Diamond");
            rankList.add("Master");
        } else if (rank.equals("Master")) {
            rankList.add("Diamond");
            rankList.add("Master");
            rankList.add("GrandMaster");
        } else if (rank.equals("GrandMaster")) {
            rankList.add("Master");
            rankList.add("GrandMaster");
        } else if (rank.equals("Challenger")) {
            rankList.add("Challenger");
        }

        // Redis Data List 출력
        List<Object> queueList = operations.opsForList().range("queueList", 1, -1);
        List<String> rankFilterList = new ArrayList<>();

        // 계급 필터링
        for (Object key : queueList) {
            String name = String.valueOf(key).split("_")[0];
            System.out.println("rank : " + name);
            for (int i = 0; i < rankList.size(); i++) {
                if(name.equals(rankList.get(i))) {
                    rankFilterList.add(key.toString());
                }
            }
        }    
        // mmr 범위 조정
        for(String fileterList : rankFilterList) {
            String minRange = String.valueOf(fileterList).split("_")[1];
            String maxRange = String.valueOf(fileterList).split("_")[2];

            System.out.println("min : " + minRange);
            System.out.println("max : " + maxRange);
            
            if (Integer.parseInt(minRange) <= mmr) {
                if (Integer.parseInt(maxRange) >= mmr) {
                    System.out.println("범위 안에 잘 들어옴 큐 이름을 반환" + fileterList);
                    queueName = fileterList;
                    return queueName;
                }
            }           
        }

        queueName = rank+"_"+min+"_"+max;
        return queueName;
    }

}
