package com.lol.match.main.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
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
    public String match(@RequestBody UserMatchDto userMatchDto) throws InterruptedException {

        int mmr = userMatchDto.getMmr();

        // TODO : 해쉬맵이 있는지 체크하는 메소드 추가 필요, 맵값
        String listName = isMap(mmr, userMatchDto.getRank(), userMatchDto);

        System.out.println("listName : " + listName);

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        // map size가 10보다 작을때는 계속 머무르기
        if(hashOperations.size("map:"+listName) < 10) {
            queueCheck(hashOperations.size("map:"+listName), listName);
        }

        return "OK";
    }

        // 큐 사이즈 확인 10이면 재귀 메소드 탈출
    private void queueCheck(Long size, String listName) throws InterruptedException {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        if(size < 10) {
            System.out.println("와서 뱅글뱅글 도는중 : "+size);
            Thread.sleep(1000);
            queueCheck(hashOperations.size("map:"+listName), listName);
        } 
    }

    @GetMapping("/write")
    @ResponseBody
    public String write(@RequestBody UserMatchDto userMatchDto) {

        // TODO : 규칙대로 10씩 늘리는거 150이하일때 규칙 추가
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
        // isMap(0, "");

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

    // queue에서 유저 정보 삭제 : 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우
    @GetMapping("/queue/delete")
    @ResponseBody
    public String queueListDelete(@RequestBody UserMatchDto userMatchDto) {
        // Set<String> keys = redisTemplate.keys("posts:*");
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        // redisTemplate.setValueSerializer(new StringRedisSerializer());
        hashOperations.delete("map:"+userMatchDto.getQueueName(), userMatchDto.getUserId());

        return "ok";
    }

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

    // 대전 매칭 완료하기 
    @GetMapping("/queueList/find")
    @ResponseBody
    public String queueListFind(@RequestBody UserMatchDto userMatchDto) {
        
        boolean condition = true;
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        // 계속 돌기
        try {
            while(condition) {
                if(hashOperations.get("map:"+userMatchDto.getQueueName(), userMatchDto.getUserId())==null) {
                    System.out.println("본인이 대전을 찾는 와중에 취소한 경우");
                    queueCancle(userMatchDto, hashOperations);
                    return "fail";
                }
                else {
                    queueChange(userMatchDto);
                    if(hashOperations.size("map:"+userMatchDto.getQueueName()) == Long.valueOf(10)) {
                        condition = false;
                    }
                }
                System.out.println("큐 사이즈 확인 : " + hashOperations.size("map:"+userMatchDto.getQueueName()));
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("이건 최종 큐 사이즈 : "+ hashOperations.size("map:"+userMatchDto.getQueueName()));
        System.out.println("매칭 완료");
        // 뷰단에 팀 정보를 넘겨줘야함
        
        return "ok";
    }

    // 대전 매칭 수락후 완료하기, 수락하기를 안누른 유저가 있으면 다시 대기열로 돌아가 queueList7에 첫번째로 넣어줌, 진행중
    @GetMapping("/queueList/accept")
    @ResponseBody
    public String queueListAccept(@RequestBody UserMatchDto userMatchDto) {
        
        boolean condition = true;
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        // 계속 돌기
        try {
            while(condition) {
                if(hashOperations.get("map:"+userMatchDto.getQueueName(), userMatchDto.getUserId())==null) {
                    System.out.println("본인이 대전을 찾는 와중에 취소한 경우");
                    return "fail";
                }
                else {
                    if(hashOperations.size("map:"+userMatchDto.getQueueName()) == Long.valueOf(10)) {

                        condition = false;
                    }
                }
                // System.out.println("큐 사이즈 확인 : " + hashOperations.size("map:"+userMatchDto.getQueueName()));
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO : 나중에 필요할수도 있음
        // redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        System.out.println("이건 최종 큐 사이즈 : "+ hashOperations.size("map:"+userMatchDto.getQueueName()));
        System.out.println("매칭 완료");
        // 뷰단에 팀 정보를 넘겨줘야함
        
        return "ok";
    }

    // 대전 찾기 중 실패
    private void queueCancle(UserMatchDto userMatchDto, HashOperations<String, Object, Object> hashOperations) {
        hashOperations.put("position:"+userMatchDto.getQueueName(), userMatchDto.getPosition(), 
        Integer.parseInt(hashOperations.get("position:"+userMatchDto.getQueueName(), userMatchDto.getPosition()).toString())-1);
            hashOperations.delete("map:"+userMatchDto.getQueueName(), userMatchDto.getUserId());
    }

    private void queueChange(UserMatchDto userMatchDto) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value: 직렬화에 사용할 Object 사용하기   

        LocalDateTime time = LocalDateTime.now();

		String nowTime = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String queueName = "";
		System.out.println("시간 : " + nowTime);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date nowDate = sdf.parse(nowTime);
            Date originalDate = sdf.parse(userMatchDto.getQueueName().split("_")[3]);

            if(nowDate.after(originalDate)) {
                System.out.println("리스트 및 hashMap 변경");
                // TODO : 규칙대로 10씩 늘리는거 150이하일때 규칙 추가
                String[] queueList = userMatchDto.getQueueName().split("_");
                int min = Integer.parseInt(queueList[1]) > 150 ? Integer.parseInt(queueList[1]) - 5 : 100;
                int max = Integer.parseInt(queueList[1]) + 5;
                String uuid = UUID.randomUUID().toString();
                queueName = userMatchDto.getRank()+"_"+min+"_"+max+"_"+"시간"+"_"+uuid;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    // 큐가 이미 존재하는지, 새롭게 만들어야하는지 판단
    private String isMap(int mmr, String rank, UserMatchDto userMatchDto) {

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        String queueName = "";
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        List<String> rankList = new ArrayList<>();

        // 랭킹에 따른 배치
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
        List<Object> queueList = operations.opsForList().range("queueList1", 1, -1);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        List<String> rankFilterList = new ArrayList<>();
        List<String> positionList = new ArrayList<>();

        // 계급 필터링
        for (Object key : queueList) {
            // 큐 사이즈 확인 
            if(hashOperations.size("map:"+key.toString()) < 10) {
                String name = String.valueOf(key).split("_")[0];
                // System.out.println("rank : " + name);
                for (int i = 0; i < rankList.size(); i++) {
                    if(name.equals(rankList.get(i))) {
                        rankFilterList.add(key.toString());
                    }
                }
            }
        }    
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value: 직렬화에 사용할 Object 사용하기   

        // mmr 범위 조정
        for(String fileterList : rankFilterList) {
            String minRange = String.valueOf(fileterList).split("_")[1];
            String maxRange = String.valueOf(fileterList).split("_")[2];

            if (Integer.parseInt(minRange) <= mmr) {
                if (Integer.parseInt(maxRange) >= mmr) {
                    System.out.println("범위 안에 잘 들어옴 큐 이름을 반환" + fileterList);
                    positionList.add(fileterList);
                }
            }           
        }

        if(positionList.size()>0) {
            queueName = positionCheck(positionList, userMatchDto, min, max);
        }
        else {
            // 일치하는 mmr이 없을 경우
            System.out.println("새롭게 큐를 추가함 ");
            String uuid = UUID.randomUUID().toString();
            queueName = rank+"_"+min+"_"+max+"_"+uuid;
            queueCreate(queueName, userMatchDto);
            hashOperations.put("position:"+queueName, userMatchDto.getPosition(), 1);
            redisTemplate.opsForList().rightPush("queueList1", queueName);

        }

        return queueName;
    }

    // 큐 생성
    private void queueCreate(String queueName, UserMatchDto userMatchDto) {
        userMatchDto.queueNameSet(queueName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value: 직렬화에 사용할 Object 사용하기   
        hashOperations.put("map:"+queueName, userMatchDto.getUserId(), userMatchDto);
    }

    // 포지션 확인
    private String positionCheck(List<String> positionList, UserMatchDto userMatchDto, int min, int max) {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        String queueName = "";

        for (int i = 0; i < positionList.size(); i++) {
            // 포지션 조건 추가
            if(hashOperations.hasKey("position:"+positionList.get(i), userMatchDto.getPosition())) {
                if(Integer.valueOf(hashOperations.get("position:"+positionList.get(i), userMatchDto.getPosition()).toString())>1) {
                    if(i==positionList.size()-1) {
                        String uuid = UUID.randomUUID().toString();
                        queueName = userMatchDto.getRank()+"_"+min+"_"+max+"_"+uuid;
                        queueCreate(queueName, userMatchDto);
                        System.out.println("일치하는 mmr이 있으나 포지션이 없음.");
                        System.out.println("큐 새로 생성");
                        hashOperations.put("position:"+queueName, userMatchDto.getPosition(), 1);
                        redisTemplate.opsForList().rightPush("queueList1", queueName);
                        return queueName;
                    }
                    else {
                        // 해당 큐에 포지션이 없으나, 다음 후보가 있을 경우
                        System.out.println("오니?");
                        continue;
                    }
                }
                else {
                    // 포지션 자리가 존재해 기존의 큐에 값 추가, 포지션 자리가 1인경우, 0인경우
                    queueName = positionList.get(i);
                    queueCreate(queueName, userMatchDto);
                    hashOperations.put("position:"+queueName, userMatchDto.getPosition(), 2);
                    return queueName;
                }
            }
            else {
                // 포지션 자리가 존재해 기존의 큐에 값 추가, 포지션 자리가 0인경우, 없는 경우
                queueName = positionList.get(i);
                queueCreate(queueName, userMatchDto);
                hashOperations.put("position:"+queueName, userMatchDto.getPosition(), 1);
                return queueName;
            }
        }

        return queueName;

    }

}
