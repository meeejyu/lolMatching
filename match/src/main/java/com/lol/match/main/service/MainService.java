package com.lol.match.main.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.match.domain.dto.UserMatchDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MainService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;
 
    public HashMap<String, String> match(UserMatchDto userMatchDto) throws Exception {
        
        HashMap<String, String> result = new HashMap<>();
        int mmr = userMatchDto.getMmr();
        boolean condition = true;

        // TODO : 해쉬맵이 있는지 체크하는 메소드 추가 필요, 맵값
        String listName = isMap(mmr, userMatchDto.getRank(), userMatchDto);

        System.out.println("listName : " + listName);

        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        hashOperations.put("queueAll", userMatchDto.getUserId(), listName);

        // map size가 10보다 작을때는 계속 머무르기
        if(hashOperations.size("map:"+listName) < 10) {
            condition = false;
            String status = queueCheck(hashOperations.size("map:"+listName), listName, userMatchDto.getUserId());
            if(status.equals("cancel")) {
                result.put("code", "cancel");
                return result;
            }
        }
        else {
            result.put("code", "success");
            result.put("listname", listName);    
        }
        if(condition) {
            acceptTime(listName);
        }
        return result;
    }

    // 동의 시간 저장
    private void acceptTime(String listName) throws ParseException {
        
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        Calendar cal = Calendar.getInstance();
        System.out.println("date : "+date);
        cal.setTime(date);
        cal.add(Calendar.SECOND, 10);
        System.out.println("date+10 : "+cal.getTime());
        String saveTime = simpleDateFormat.format(cal.getTime());
        System.out.println("saveTime : "+saveTime);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        hashOperations.put("acceptTime", listName, saveTime);
    }

    // queue에서 유저 정보 삭제 : 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우
    public HashMap<String, String> queueListDelete(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException {

        HashMap<String, String> result = new HashMap<>();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        Object key = hashOperations.get("queueAll", userMatchDto.getUserId());

        System.out.println(key.toString());

        // 전체 키에서 삭제
        hashOperations.delete("queueAll", userMatchDto.getUserId());

        // 잡은 큐에서 삭제
        hashOperations.delete("map:"+key.toString(), userMatchDto.getUserId());

        // 포지션 수 줄임
        Object position = hashOperations.get("position:"+key.toString(), userMatchDto.getPosition());

        if(Integer.parseInt(position.toString())==1) {
            hashOperations.delete("position:"+key.toString(), userMatchDto.getPosition());
        }
        else if(Integer.parseInt(position.toString())==2) {
            redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value: 직렬화에 사용할 Object 사용하기   
            hashOperations.put("position:"+key.toString(), userMatchDto.getPosition(), 1);
        }
    
        result.put("code", "success");

        return result;
    }
    
    // 대전 매칭 완료하기 
    public HashMap<String, String> matchAccept(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        
        // TODO : 시간 추가, 재귀 메소드 수정 -> 추가
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        String listName = userMatchDto.getQueueName();
        // 계속 돌기
        if(hashOperations.hasKey("map:"+listName, userMatchDto.getUserId())==false) {
            log.info("잘못된 요청입니다.");
            result.put("code", "fail");
            return result;
        }
        else {
            redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value: 직렬화에 사용할 Object 사용하기   
            hashOperations.put("accept:"+userMatchDto.getQueueName(), userMatchDto.getUserId(), userMatchDto);
            
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

            Date date = new Date();
            System.out.println("date : "+date);
    
            redisTemplate.setHashValueSerializer(new StringRedisSerializer());
    
            Object object = hashOperations.get("acceptTime", listName);
    
            Date saveDate = simpleDateFormat.parse(object.toString());
            
            while(saveDate.after(date)) {
                
                Thread.sleep(1000);
                Date newDate = new Date();
                System.out.println("결과 : "+saveDate.after(newDate));
                
                // 10초가 지날때까지 사이즈 검토
                if(saveDate.after(newDate)==false) {
                    break;
                }
                // 10초가 안지났지만 사이즈가 10되면 미리 바로 탈출
                if(hashOperations.size("accept:"+userMatchDto.getQueueName())==10) {
                    result.put("code", "success");
                    result.put("listname", listName);
                    return result;
                }
            }
            // 한번 더 사이즈 검토
            if(hashOperations.size("accept:"+userMatchDto.getQueueName())==10) {
                result.put("code", "success");
                result.put("listname", listName);
            }
            else {
                result.put("code", "fail");
            }
            return result;
        }

    }

    // 팀 가르고 팀 정보 저장하기
    public HashMap<String, String> matchComplete(UserMatchDto userMatchDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        
        hashOperations.values("accept");
        return result;
    }



    // 매칭 진행 : 큐 사이즈 확인 10이면 재귀 메소드 탈출
    private String queueCheck(Long size, String listName, String id) throws InterruptedException, JsonMappingException, JsonProcessingException {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        Object object = hashOperations.get("map:"+listName, id);
        if(object==null) {
            return "cancel";
        }
        else {
            if(size < 10) {
                System.out.println("와서 뱅글뱅글 도는중 : "+size);
                Thread.sleep(1000);
                String status = queueCheck(hashOperations.size("map:"+listName), listName, id);
                if(status.equals("cancel")) {
                    return "cancel";
                }
            }
            return "ok";
        }
    }

    // 큐가 이미 존재하는지, 새롭게 만들어야하는지 판단
    private String isMap(int mmr, String rank, UserMatchDto userMatchDto) throws Exception {

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
        else {
            throw new Exception("잘못된 요청입니다");
        }

        // Redis Data List 출력
        Long listSize = operations.opsForList().size("queueList1");
        List<Object> queueList = operations.opsForList().range("queueList1", 0, listSize-1);

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
                        System.out.println("큐 이름 테스트 : "+queueName);
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
                    System.out.println("큐 이름 테스트 : "+queueName);
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
}
