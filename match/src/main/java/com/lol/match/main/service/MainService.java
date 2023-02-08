package com.lol.match.main.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.match.common.exception.BusinessLogicException;
import com.lol.match.common.exception.ExceptionCode;
import com.lol.match.main.mapper.MainMapper;
import com.lol.match.main.model.GroupMatchDto;
import com.lol.match.main.model.SettingAllDto;
import com.lol.match.main.model.UserAllDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MainService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final MainMapper mainMapper;

    private final ObjectMapper objectMapper;
 
    public HashMap<String, String> match(int userId) throws Exception {

        // DB로 세팅 정보 가져오기
        SettingAllDto settingDto = mainMapper.findBySettingId();

        // mmr만 고려하여 매칭 시켜주는 경우
        // mmrIsMap(userAllDto);

        // mmr, rank, position 고려하여 매칭 시켜주는 경우
        return allIsMap(userId, settingDto);

        // mmr, rank 매칭 시켜주는 경우
        // rankIsMap(userAllDto);

        // mmr, position 고려하여 매칭 시켜주는 경우
        // positionIsMap(userAllDto);

        // 두번 요청했을 경우, 예외
        
    }

    private void positionIsMap(UserAllDto userAllDto) {
    }

    private void rankIsMap(UserAllDto userAllDto) {
    }

    private void mmrIsMap(UserAllDto userAllDto) {
    }

    private HashMap<String, String> allIsMap(int userId, SettingAllDto settingDto) throws Exception {

        HashMap<String, String> result = new HashMap<>();
        boolean condition = true;
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        
        UserAllDto userAllDto = mainMapper.findByAllUserId(userId);

        int mmr = userAllDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("queueAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String listName = isMap(mmr, userAllDto, settingDto);

            System.out.println("listName : " + listName);

            hashOperations.put("queueAll", id, listName);

            // map size가 10보다 작을때는 계속 머무르기
            if(hashOperations.size("map:"+listName) < 10) {
                condition = false;

                // 중도 취소 여부 확인
                String status = queueCheck(hashOperations.size("map:"+listName), listName, userAllDto);
                if(status.equals("cancel")) {
                    result.put("code", "cancel");
                    return result;
                }
            }
            result.put("code", "success");
            result.put("listname", listName);    
            if(condition) {
                // 매칭 동의 시간 추가
                acceptTime(listName);
            }
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

        hashOperations.put("acceptTime", listName, saveTime);
    }

    // queue에서 유저 정보 삭제 : 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : ALL
    public HashMap<String, String> queueListDelete(int userId) throws Exception {

        HashMap<String, String> result = new HashMap<>();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        String id = Integer.toString(userId);
        if(hashOperations.hasKey("queueAll", id)) {
            Object key = hashOperations.get("queueAll", id);

            UserAllDto user = objectMapper.readValue(hashOperations.get("map:"+key.toString(), id).toString(), UserAllDto.class);

            String position = user.getUserPosition();

            // 전체 키에서 삭제
            hashOperations.delete("queueAll", id);

            // 잡은 큐에서 삭제
            hashOperations.delete("map:"+key.toString(), id);
    
            // 포지션 수 줄임
            Object userPosition = hashOperations.get("position:"+key.toString(), position);
    
            if(Integer.parseInt(userPosition.toString())==1) {
                hashOperations.delete("position:"+key.toString(), position);
            }
            else if(Integer.parseInt(userPosition.toString())==2) {
                hashOperations.put("position:"+key.toString(), userPosition, "1");
            }
        
            result.put("code", "success");
        }
        else {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }

        return result;
    }
    
    // 대전 매칭 완료하기 
    public HashMap<String, String> matchAccept(int userId) throws Exception {
        
        // TODO : 시간 추가, 재귀 메소드 수정 -> 추가
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        boolean condition = true;

        String id = Integer.toString(userId);
        
        // 요청 검증
        if(hashOperations.hasKey("queueAll", id)==false) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        String queueName = hashOperations.get("queueAll", id).toString();

        // 두번 요청했을때 예외 처리
        if(hashOperations.hasKey("accept:"+queueName, id)==true) {
            throw new BusinessLogicException(ExceptionCode.DUPLICATION_REQUEST);
        }
        Date date = new Date();
        System.out.println("date : "+date);

        Object object = hashOperations.get("acceptTime", queueName);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

        Date saveDate = simpleDateFormat.parse(object.toString());

        if(saveDate.after(date)==false) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String userAll = hashOperations.get("map:"+queueName, id).toString();
            hashOperations.put("accept:"+queueName, id, userAll);
            Long beforeSize = hashOperations.size("accept:"+queueName);
            
            while(saveDate.after(date)) {
                
                Thread.sleep(1000);
                Date newDate = new Date();
                System.out.println("결과 : "+saveDate.after(newDate));
                
                // 10초가 지날때까지 사이즈 검토
                if(saveDate.after(newDate)==false) {
                    break;
                }
                if(hashOperations.size("accept:"+queueName)<10) {
                    condition = false;
                    continue;
                }
                // 10초가 안지났지만 사이즈가 10되면 미리 바로 탈출
                if(hashOperations.size("accept:"+queueName)==10) {
                    result.put("code", "success");
                    result.put("listname", queueName);
                    if(condition) {
                        teamDivide(queueName);
                        // 팀 정보 제외 전체 삭제
                        // delete(listName);                        
                    }
                    return result;
                }
            }
            // 시간안에 모두 동의하지 않은 경우
            result.put("code", "fail");
            result.put("message", "수락하지 않은 유저가 있습니다. 다시 대기열로 돌아갑니다.");
            Long afterSize = hashOperations.size("accept:"+queueName);
            if(beforeSize==afterSize) {

                Map<Object, Object> userMap = hashOperations.entries("map:"+queueName);

                for(Object key : userMap.keySet()) {
                    if(hashOperations.hasKey("accept:"+queueName, key)==false) {
                        UserAllDto userAllDto = objectMapper.readValue(hashOperations.get("map:"+queueName, key).toString(), UserAllDto.class);
                        positionDelete(userAllDto.getUserPosition(), queueName);
                        hashOperations.delete("map:"+queueName, key);
                        hashOperations.delete("queueAll", key);
                    }
                }
                hashOperations.getOperations().delete("accept:"+queueName);

            }

            return result;
        }
    }

    // 포지션 지우기
    private void positionDelete(String userPosition, String queueName) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Object object = hashOperations.get("position:"+queueName, userPosition);
        
        if(object.toString().equals("2")) {
            hashOperations.put("position:"+queueName, userPosition, "1");
        }
        else {
            hashOperations.delete("position:"+queueName, userPosition);
        }
    }

    private void teamDivide(String listName) throws JsonMappingException, JsonProcessingException {
        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        Map<Object, Object> map = hashOperations.entries("accept:"+listName);

        // list는 키값만 저장, map은 키값이랑 mmr 저장
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
            UserAllDto user = objectMapper.readValue(map.get(key).toString(), UserAllDto.class);
            int mmr = user.getUserMmr();
            String position = user.getUserPosition();
            if(position.equals("top")) {
                topMap.put(key, mmr);
                topList.add(key);
            }
            else if(position.equals("support")) {
                supportMap.put(key, mmr);
                supportList.add(key);
            }
            else if(position.equals("mid")) {
                midMap.put(key, mmr);
                midList.add(key);
            }
            else if(position.equals("jungle")) {
                junglepMap.put(key, mmr);
                jungleList.add(key);
            }
            else if(position.equals("bottom")) {
                bottomMap.put(key, mmr);
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
            Object objectA = hashOperations.get("accept:"+listName, aList[i]);
            Object objectB = hashOperations.get("accept:"+listName, bList[i]);
            hashOperations.put("teamA:"+listName, aList[i], objectA);
            hashOperations.put("teamB:"+listName, bList[i], objectB);
        }
    }

    // 팀 배정 정보 및 본인이 속한 팀 정보 주기, 에외 처리 고민
    public GroupMatchDto matchComplete(int userId, String queueName) throws Exception {
        
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        String id = Integer.toString(userId);

        String userInfo;

        if(hashOperations.hasKey("teamA:"+queueName, id)) {
            userInfo = "A";
        }
        else if(hashOperations.hasKey("teamB:"+queueName, id)){
            userInfo = "B";
        }
        else {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        Map<Object, Object> teamAMap = hashOperations.entries("teamA:"+queueName);
        Map<Object, Object> teamBMap = hashOperations.entries("teamB:"+queueName);

        GroupMatchDto groupMatchDto = new GroupMatchDto(userInfo, teamInfo(teamAMap), teamInfo(teamBMap));
        
        return groupMatchDto;

    }

    // 팀 정보 List 형태로 저장
    private List<UserAllDto> teamInfo(Map<Object, Object> teamMap) throws JsonMappingException, JsonProcessingException {
        int count = 0;

        List<UserAllDto> teamList = new ArrayList<>();

        for(Object key : teamMap.keySet()) {
            UserAllDto userAllDto = objectMapper.readValue(teamMap.get(key).toString(), UserAllDto.class);
            teamList.add(count, userAllDto);
            count += 1;
        }

        return teamList;
    }

    // 매칭 진행 : 큐 사이즈 확인 10이면 재귀 메소드 탈출
    private String queueCheck(Long size, String listName, UserAllDto userAllDto) throws InterruptedException, JsonMappingException, JsonProcessingException {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        
        if(hashOperations.hasKey("map:"+listName, Integer.toString(userAllDto.getUserId()))==false) {
            return "cancel";
        }
        else {
            if(size < 10) {
                System.out.println("와서 뱅글뱅글 도는중 : "+size);
                Thread.sleep(1000);
                String status = queueCheck(hashOperations.size("map:"+listName), listName, userAllDto);
                if(status.equals("cancel")) {
                    return "cancel";
                }
            }
            return "ok";
        }
    }

    // 큐가 이미 존재하는지, 새롭게 만들어야하는지 판단
    private String isMap(int mmr, UserAllDto userAllDto, SettingAllDto settingDto) throws Exception {

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        String rank = userAllDto.getUserRank();

        String queueName = "";
        int min = mmr > 150 ? mmr - 50 : 100;
        int max = mmr + 50;

        List<String> rankList = new ArrayList<>();

        // 랭크 계급 설정
        rankList = rankListAdd(rank, settingDto);
        
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
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }

        // Redis Data List 출력
        Long listSize = operations.opsForList().size("queueList");
        List<Object> queueList = operations.opsForList().range("queueList", 0, listSize-1);

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        List<String> rankFilterList = new ArrayList<>();
        List<String> positionList = new ArrayList<>();

        // 계급 필터링
        for (Object key : queueList) {
            // 큐 사이즈 확인 
            if(hashOperations.size("map:"+key.toString()) < 10 && hashOperations.size("map:"+key.toString()) > 0) {
                String name = String.valueOf(key).split("_")[0];
                // System.out.println("rank : " + name);
                for (int i = 0; i < rankList.size(); i++) {
                    if(name.equals(rankList.get(i))) {
                        rankFilterList.add(key.toString());
                    }
                }
            }
        }    

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
            queueName = positionCheck(positionList, userAllDto, min, max);
        }
        else {
            // 일치하는 mmr이 없을 경우
            System.out.println("새롭게 큐를 추가함 ");
            String uuid = UUID.randomUUID().toString();
            queueName = rank+"_"+min+"_"+max+"_"+uuid;
            queueCreate(queueName, userAllDto);
            hashOperations.put("position:"+queueName, userAllDto.getUserPosition(), "1");
            redisTemplate.opsForList().rightPush("queueList", queueName);

        }

        return queueName;
    }

    private List<String> rankListAdd(String rank, SettingAllDto settingDto) {
        
        
        List<String> rankList = new ArrayList<>();
        
        return null;
    }

    // 큐 생성
    private void queueCreate(String queueName, UserAllDto userAllDto) throws JsonProcessingException {
        String id = Integer.toString(userAllDto.getUserId());
        
        userAllDto.queueNameSet(queueName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userAllDto);
        hashOperations.put("map:"+queueName, id, user);
    }

    // 포지션 확인
    private String positionCheck(List<String> positionList, UserAllDto userAllDto, int min, int max) throws JsonProcessingException {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String queueName = "";

        for (int i = 0; i < positionList.size(); i++) {
            // 포지션 조건 추가
            if(hashOperations.hasKey("position:"+positionList.get(i), userAllDto.getUserPosition())) {
                if(Integer.valueOf(hashOperations.get("position:"+positionList.get(i), userAllDto.getUserPosition()).toString())>1) {
                    if(i==positionList.size()-1) {
                        String uuid = UUID.randomUUID().toString();
                        queueName = userAllDto.getUserRank()+"_"+min+"_"+max+"_"+uuid;
                        queueCreate(queueName, userAllDto);
                        System.out.println("일치하는 mmr이 있으나 포지션이 없음.");
                        System.out.println("큐 새로 생성");
                        hashOperations.put("position:"+queueName, userAllDto.getUserPosition(), "1");
                        redisTemplate.opsForList().rightPush("queueList", queueName);
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
                    queueCreate(queueName, userAllDto);
                    System.out.println("큐 이름 테스트 : "+queueName);
                    hashOperations.put("position:"+queueName, userAllDto.getUserPosition(), "2");
                    return queueName;
                }
            }
            else {
                // 포지션 자리가 존재해 기존의 큐에 값 추가, 포지션 자리가 0인경우, 없는 경우
                queueName = positionList.get(i);
                queueCreate(queueName, userAllDto);
                hashOperations.put("position:"+queueName, userAllDto.getUserPosition(), "1");
                return queueName;
            }
        }

        return queueName;

    }

    // 큐를 지우기
    public void delete(String listName) {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        int size = hashOperations.entries("map:"+listName).size();

        hashOperations.getOperations().delete("map:"+listName);        
        hashOperations.getOperations().delete("position:"+listName);
        hashOperations.getOperations().delete("accept:"+listName);
        hashOperations.delete("acceptTime", listName);

        Map<Object, Object> allMap = hashOperations.entries("queueAll");
        int count = 0;
        
        for(Object key : allMap.keySet()) {
            if(hashOperations.get("queueAll", key).toString().equals(listName)) {
                hashOperations.delete("queueAll", key);
                count += 1;
            }
            // 큐에 들어있는 사이즈만큼 다 지우면 바로 탈출
            if(count==size) {
                break;
            }
        }
    }

}
