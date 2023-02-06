package com.lol.match.main.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.match.main.model.UserMatchDto;
import com.lol.match.main.service.MainService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final RedisTemplate<String, Object> redisTemplate;

    private final MainService mainService;

    private final ObjectMapper objectMapper;


    @PostMapping("/main")
    public String main(UserMatchDto userMatchDto, Model model) {

        System.out.println("회원정보 : "+ userMatchDto.toString());
        model.addAttribute("userMatchDto", userMatchDto);

        return "main";
    }

    // 매칭 하기
    // TODO : TEST
    // db 접속 정보를 가저와서 세팅하는걸로 수정
    @PostMapping("/match")
    @ResponseBody
    public HashMap<String, String> match(UserMatchDto userMatchDto) throws Exception {

        HashMap<String, String> result = mainService.match(userMatchDto);

        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : queue에서 해당 유저 정보 삭제
    @PostMapping("/queue/cancel")
    @ResponseBody
    public HashMap<String, String> queueListDelete(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException {

        HashMap<String, String> result = mainService.queueListDelete(userMatchDto);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/accept")
    @ResponseBody
    public HashMap<String, String> matchAccept(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        
        HashMap<String, String> result = mainService.matchAccept(userMatchDto);
        
        return result;
    }

    // TODO : TEST
    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/complete")
    @ResponseBody
    public HashMap<String, String> matchComplete(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        
        HashMap<String, String> result = mainService.matchComplete(userMatchDto);
        
        return result;
    }

    // 큐를 지우고 싶을때
    @DeleteMapping("/queue/delete/{listName}")
    @ResponseBody
    public String delete(@PathVariable String listName) {
        
        mainService.delete(listName);

        return "ok";
    }



    // 대전 매칭 수락후 완료하기, 수락하기를 안누른 유저가 있으면 다시 대기열로 돌아가 queueList7에 첫번째로 넣어줌, 진행중
    // @GetMapping("/queueList/accept")
    // @ResponseBody
    // public String queueListAccept(@RequestBody UserMatchDto userMatchDto) {
        
    //     String result = mainService.queueListAccept(userMatchDto);
        
    //     return result;
    // }


















    @GetMapping("/position")
    @ResponseBody
    public String position(@RequestBody UserMatchDto userMatchDto) {
        // Set<String> keys = redisTemplate.keys("posts:*");
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        List<Object> list = hashOperations.values("map:"+userMatchDto.getQueueName());

        try {
            for (int i = 0; i < list.size(); i++) {
                UserMatchDto user = objectMapper.readValue(list.get(i).toString(), UserMatchDto.class);
                // hash를 사용해서 포지션 관리값 넣기
                System.out.println(i+"번째 유저값 나와라 : "+ user.toString());
                System.out.println(user.toString());   
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        // System.out.println(list.toString());
        return "ok";
    }
    
    @GetMapping("/write")
    @ResponseBody
    public String write(@RequestBody UserMatchDto userMatchDto) {

        // TODO : 규칙대로 10씩 늘리는거 150이하일때 규칙 추가
        int mmr = userMatchDto.getMmr();
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        String listname = userMatchDto.getRank() + "_" + min + "_" + max;

        // // String listname = "queue";

        // int time = 1;
        // GroupMatchDto groupMatchDto = new GroupMatchDto(listname, max, min, time);

        // redisTemplate.opsForList().leftPush(listname, userMatchDto);
        // redisTemplate.opsForList().leftPush("queue", groupMatchDto);

        // RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        // List<Object> a = operations.opsForList().range(listname, 0, 9);

        // // 리스트는 있고, 11명이 되려고 할때 1초 쓰레드 추가
        // try {

        //     for (int i = 0; i < a.size(); i++) {
        //         UserMatchDto userMatchDto2 = objectMapper.readValue(a.get(0).toString(), UserMatchDto.class);
        //         System.out.println("값 나와랏! : " + userMatchDto2.toString());
        //     }

        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
        return "OK";
    }

    @GetMapping("/key")
    @ResponseBody
    public String key() throws ParseException, JsonMappingException, JsonProcessingException {
        
        return "ok";
    }


    @GetMapping("/test")
    @ResponseBody
    public String test(String listName) throws ParseException, InterruptedException {
        
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

        Date date = new Date();
        System.out.println("date : "+date);

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        Object object = hashOperations.get("acceptTime", listName);

        Date saveDate = simpleDateFormat.parse(object.toString());
        
        while(saveDate.after(date)) {
            
            Thread.sleep(1000);
            Date newDate = new Date();
            System.out.println("결과 : "+saveDate.after(newDate));
            if(saveDate.after(newDate)==false) {
                break;
            }
        }

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

}
