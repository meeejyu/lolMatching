package com.lol.match.main.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.match.domain.dto.UserMatchDto;
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

    @PostMapping("/match")
    @ResponseBody
    public HashMap<String, String> match(UserMatchDto userMatchDto) throws Exception {

        HashMap<String, String> result = mainService.match(userMatchDto);

        return result;
    }

    // queue에서 유저 정보 삭제 : 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우
    @PostMapping("/queue/delete")
    @ResponseBody
    public HashMap<String, String> queueListDelete(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException {

        HashMap<String, String> result = mainService.queueListDelete(userMatchDto);

        return result;
    }

    // 대전 매칭 완료하기 
    @PostMapping("/match/accept")
    @ResponseBody
    public HashMap<String, String> matchAccept(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        
        HashMap<String, String> result = mainService.matchAccept(userMatchDto);
        
        return result;
    }

    // 대전 매칭 완료 후 팀갈라서 각자 팀 보여주기
    @PostMapping("/match/complete")
    @ResponseBody
    public String matchComplete(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        
        HashMap<String, String> result = mainService.matchComplete(userMatchDto);
        
        return "matchSuccess";
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
        
        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        
        Map<Object, Object> map = hashOperations.entries("map:Gold_520_620_c0dc9b5d-71e9-4cc2-aa03-5e31a6bfcaaa");

        Map<Object, Integer> topMap = new HashMap<>();
        List<Object> topList = new ArrayList<>();

        Map<Object, Integer> junglepMap = new HashMap<>();
        List<Object> jungleList = new ArrayList<>();

        Map<Object, Integer> midMap = new HashMap<>();
        List<Object> midList = new ArrayList<>();

        Map<Object, Integer> bottomMap = new HashMap<>();
        List<Object> bottomList = new ArrayList<>();

        Map<Object, Integer> supportMap = new HashMap<>();
        List<Object> supportList = new ArrayList<>();

        Map<Integer, List<String>> teamCom = new HashMap<>();        

        // postion이랑 mmr 얻어와서 비교하기
        for(Object key : map.keySet() ){
            UserMatchDto user = objectMapper.readValue(map.get(key).toString(), UserMatchDto.class);
            if(key.toString().contains("top")) {
                topMap.put(key, user.getMmr());
                topList.add(key);
            }
            else if(key.toString().contains("support")) {
                supportMap.put(key, user.getMmr());
                supportList.add(key);
            }
            else if(key.toString().contains("mid")) {
                midMap.put(key, user.getMmr());
                midList.add(key);
            }
            else if(key.toString().contains("jungle")) {
                junglepMap.put(key, user.getMmr());
                jungleList.add(key);
            }
            else if(key.toString().contains("bottom")) {
                bottomMap.put(key, user.getMmr());
                bottomList.add(key);
            }
        }

        for (int i = 0; i < topMap.size(); i++) {
            int topA = topMap.get(topList.get(i));
            for (int j = 0; j < supportMap.size(); j++) {
                int supportA = supportMap.get(supportList.get(j));    
                for (int q = 0; q < midMap.size(); q++) {
                    int midA = midMap.get(midList.get(q));    
                    for (int e = 0; e < junglepMap.size(); e++) {
                        int jungleA = junglepMap.get(jungleList.get(e));    
                        for (int r = 0; r < bottomMap.size(); r++) {
                            int bottomA = bottomMap.get(bottomList.get(r)); 
                            int sumA = topA + supportA + midA + jungleA + bottomA;
                            int topB = 0;
                            int supportB = 0;
                            int midB = 0;
                            int jungleB = 0;
                            int bottomB = 0;
                            String aList = topList.get(i)+"/"+supportList.get(j)+"/"+midList.get(q)+"/"+jungleList.get(e)+"/"+bottomList.get(r);
                            
                            // 팀 B의 리스트 설정, 팀 A와 반대되는 유저 추가
                            String bList = "";
                            if(i==0) {
                                topB = topMap.get(topList.get(1));
                                bList += topList.get(1);
                            }
                            else if(i==1) {
                                topB = topMap.get(topList.get(0));
                                bList += topList.get(0);
                            }
                            if(j==0) {
                                supportB = supportMap.get(supportList.get(1)); 
                                bList += "/"+supportList.get(1);
                            }
                            else if(j==1) {
                                supportB = supportMap.get(supportList.get(0)); 
                                bList += "/"+supportList.get(0);
                            }
                            if(q==0) {
                                midB = midMap.get(midList.get(1)); 
                                bList += "/"+midList.get(1);
                            }
                            else if(q==1) {
                                midB = midMap.get(midList.get(0)); 
                                bList += "/"+midList.get(0);
                            }
                            if(e==0) {
                                jungleB = junglepMap.get(jungleList.get(1));  
                                bList += "/"+jungleList.get(1);
                            }
                            else if(e==1) {
                                jungleB = junglepMap.get(jungleList.get(0));  
                                bList += "/"+jungleList.get(0);
                            }
                            if(r==0) {
                                bottomB = bottomMap.get(bottomList.get(1)); 
                                bList += "/"+bottomList.get(1);
                            }
                            else if(r==1) {
                                bottomB = bottomMap.get(bottomList.get(0)); 
                                bList += "/"+bottomList.get(0);
                            }
                            int sumB = topB + supportB + midB + jungleB + bottomB;
                            int sumDif = Math.abs(sumA - sumB);
                            List<String> teamList = new ArrayList<>();
                            
                            teamList.add(aList);
                            teamList.add(bList);
                            if(sumDif==0) {
                                teamCom.put(sumDif, teamList);
                                break;
                            }
                            // else {
                            if(teamCom.size() > 0) {
                                for(Integer key : teamCom.keySet() ){
                                    if(key > sumDif) {
                                        teamCom.remove(key);
                                        teamCom.put(sumDif, teamList);
                                    }
                                }
                            }
                            if(teamCom.size() == 0) {
                                teamCom.put(sumDif, teamList);
                            }
                                
                            // }
                        }
                    }
                }
            }    
        }
        
        String[] aList = {};
        String[] bList = {};
        for(Integer key : teamCom.keySet() ){
            System.out.println("mmr 차이 : "+key);
            aList = teamCom.get(key).get(0).split("/");
            bList = teamCom.get(key).get(1).split("/");
        }
        for (int i = 0; i < 5; i++) {
            System.out.println("띠용? : "+aList[i]);
            // Object objectA = hashOperations.get("accept:Gold_520_620_f7fe6169-8f2c-4491-b0c6-6a8079e790f2", aList[i]);
            // Object objectB = hashOperations.get("accept:Gold_520_620_f7fe6169-8f2c-4491-b0c6-6a8079e790f2", bList[i]);
            Object objectA = hashOperations.get("map:Gold_520_620_c0dc9b5d-71e9-4cc2-aa03-5e31a6bfcaaa", aList[i]);
            Object objectB = hashOperations.get("map:Gold_520_620_c0dc9b5d-71e9-4cc2-aa03-5e31a6bfcaaa", bList[i]);
            hashOperations.put("teamA:Gold_520_620_c0dc9b5d-71e9-4cc2-aa03-5e31a6bfcaaa", aList[i], objectA);
            hashOperations.put("teamB:Gold_520_620_c0dc9b5d-71e9-4cc2-aa03-5e31a6bfcaaa", bList[i], objectB);
        }

        return "ok";
    }

    private void teamDivid(Map<String, Integer> bottomMap) {
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
