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
import com.lol.match.main.model.PositionDto;
import com.lol.match.main.model.RankDto;
import com.lol.match.main.model.SettingDto;
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
        SettingDto settingDto = mainMapper.findBySettingId();

        // mmr만 고려하여 매칭 시켜주는 경우 -> 완료
        return mmrIsMap(userId, settingDto);

        // mmr, rank, position 고려하여 매칭 시켜주는 경우 -> 완료
        // return allIsMap(userId, settingDto);

        // mmr, rank 매칭 시켜주는 경우 -> 완료
        // return rankIsMap(userId, settingDto);

        // mmr, position 고려하여 매칭 시켜주는 경우
        // return positionIsMap(userId, settingDto);

        // 두번 요청했을 경우, 예외
        
    }

    private HashMap<String, String> mmrIsMap(int userId, SettingDto settingDto) throws JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        UserAllDto userAllDto = mainMapper.findByAllUserId(userId);

        int mmr = userAllDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("queueAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String queueName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;

            Long listSize = operations.opsForList().size("queueList");
            List<Object> queueList = operations.opsForList().range("queueList", 0, listSize-1);

            for(Object key : queueList) {
                String minRange = key.toString().split("_")[0];
                String maxRange = key.toString().split("_")[1];
    
                if (Integer.parseInt(minRange) <= mmr) {
                    if (Integer.parseInt(maxRange) >= mmr) {
                        queueName = key.toString();
                        queueCreate(queueName, userAllDto);
                        break;
                    }
                }           
            }
            if(queueName.equals("")) {
                String uuid = UUID.randomUUID().toString();
                queueName = min+"_"+max+"_"+uuid;
                queueCreate(queueName, userAllDto);
                redisTemplate.opsForList().rightPush("queueList", queueName);
            }

            result = queueAddResult(id, queueName, userAllDto, settingDto);

        }
        return result;
    }

    private HashMap<String, String> queueAddResult(String id, String queueName, UserAllDto userAllDto, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        HashMap<String, String> result = new HashMap<>();
        boolean condition = true;

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        hashOperations.put("queueAll", id, queueName);

        // map size가 정원보다 작을때는 계속 머무르기
        if(hashOperations.size("map:"+queueName) < (settingDto.getSettingHeadcount()*2)) {
            condition = false;

            // 중도 취소 여부 확인
            String status = queueCheck(hashOperations.size("map:"+queueName), queueName, userAllDto, (settingDto.getSettingHeadcount()*2));
            if(status.equals("cancel")) {
                result.put("code", "cancel");
                return result;
            }
        }
        result.put("code", "success");
        result.put("listname", queueName);    
        if(condition) {
            // 매칭 동의 시간 추가
            acceptTime(queueName, settingDto.getSettingTime());
        }

        return result;
    }

    private HashMap<String, String> positionIsMap(int userId, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        UserAllDto userAllDto = mainMapper.findByAllUserId(userId);

        int mmr = userAllDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("queueAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String queueName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;

            String position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();

            List<String> positionList = new ArrayList<>();

            Long listSize = operations.opsForList().size("queueList");
            List<Object> queueList = operations.opsForList().range("queueList", 0, listSize-1);

            for(Object key : queueList) {
                String minRange = key.toString().split("_")[0];
                String maxRange = key.toString().split("_")[1];
    
                if (Integer.parseInt(minRange) <= mmr) {
                    if (Integer.parseInt(maxRange) >= mmr) {
                        positionList.add(key.toString());
                    }
                }           
            }
            if(positionList.size()>0) {
                queueName = positionCheck(positionList, userAllDto, min, max, false);
            }
            else {
                String uuid = UUID.randomUUID().toString();
                queueName = min+"_"+max+"_"+uuid;
                queueCreate(queueName, userAllDto);
                hashOperations.put("position:"+queueName, position, "1");
                redisTemplate.opsForList().rightPush("queueList", queueName);
            }
            result = queueAddResult(id, queueName, userAllDto, settingDto);
        }

        return result;
    }

    private HashMap<String, String> rankIsMap(int userId, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        UserAllDto userAllDto = mainMapper.findByAllUserId(userId);

        int mmr = userAllDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("queueAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String queueName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;
    
            RankDto rankdto = mainMapper.findByRankId(userAllDto.getRankId());

            List<String> rankList = new ArrayList<>();
            List<String> rankFilterList = new ArrayList<>();
    
            // 랭크 계급 설정
            rankList = rankListAdd(rankdto);

            Long listSize = operations.opsForList().size("queueList");
            List<Object> queueList = operations.opsForList().range("queueList", 0, listSize-1);

            // 랭크 필터링
            for (Object key : queueList) {
                // 큐 사이즈 확인 
                if(hashOperations.size("map:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("map:"+key.toString()) > 0) {
                    String name = String.valueOf(key).split("_")[0];
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
                        queueName = fileterList;
                        queueCreate(queueName, userAllDto);
                        break;
                    }
                }           
            }

            if(queueName.equals("")) {
                String uuid = UUID.randomUUID().toString();
                queueName = rankdto.getRankName()+"_"+min+"_"+max+"_"+uuid;
                queueCreate(queueName, userAllDto);
                redisTemplate.opsForList().rightPush("queueList", queueName);
            }

            result = queueAddResult(id, queueName, userAllDto, settingDto);

        }
        return result;
    }



    private HashMap<String, String> allIsMap(int userId, SettingDto settingDto) throws Exception {

        HashMap<String, String> result = new HashMap<>();
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        
        UserAllDto userAllDto = mainMapper.findByAllUserId(userId);

        int mmr = userAllDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("queueAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            RankDto rankdto = mainMapper.findByRankId(userAllDto.getRankId());
            String position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();
    
            String queueName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;
    
            List<String> rankList = new ArrayList<>();
    
            // 랭크 계급 설정
            rankList = rankListAdd(rankdto);
            
            // Redis Data List 출력
            Long listSize = operations.opsForList().size("queueList");
            List<Object> queueList = operations.opsForList().range("queueList", 0, listSize-1);
    
            List<String> rankFilterList = new ArrayList<>();
            List<String> positionList = new ArrayList<>();
    
            // 랭크 필터링
            for (Object key : queueList) {
                // 큐 사이즈 확인 
                if(hashOperations.size("map:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("map:"+key.toString()) > 0) {
                    String name = String.valueOf(key).split("_")[0];
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
                        positionList.add(fileterList);
                    }
                }           
            }
    
            if(positionList.size()>0) {
                queueName = positionCheck(positionList, userAllDto, min, max, true);
            }
            else {
                // 일치하는 mmr이 없을 경우
                System.out.println("새롭게 큐를 추가함 ");
                String uuid = UUID.randomUUID().toString();
                queueName = rankdto.getRankName()+"_"+min+"_"+max+"_"+uuid;
                queueCreate(queueName, userAllDto);
                hashOperations.put("position:"+queueName, position, "1");
                redisTemplate.opsForList().rightPush("queueList", queueName);
    
            }

            result =  queueAddResult(id, queueName, userAllDto, settingDto);

        }
        return result;
    }

    // 동의 시간 저장
    private void acceptTime(String listName, int time) throws ParseException {
        
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        Calendar cal = Calendar.getInstance();
        System.out.println("date : "+date);
        cal.setTime(date);
        cal.add(Calendar.SECOND, time);
        System.out.println("date+time : "+cal.getTime());
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

            String position = mainMapper.findByPositionId(user.getPositionId()).getPositionName();

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
        
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        boolean condition = true;

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingId();

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
                
                Date newDate = new Date();
                System.out.println("결과 : "+saveDate.after(newDate));
                
                // 설정한 시간이 지날때까지 사이즈 검토
                if(saveDate.after(newDate)==false) {
                    condition = false;
                    break;
                }
                if(hashOperations.size("accept:"+queueName) < (settingDto.getSettingHeadcount()*2)) {
                    condition = false;
                    continue;
                }
                // 설정한 시간이 안지났지만 정원이 다 차면 미리 바로 탈출
                if(hashOperations.size("accept:"+queueName) == (settingDto.getSettingHeadcount()*2)) {
                    result.put("code", "success");
                    result.put("listname", queueName);
                    if(condition) {
                        if(settingDto.getSettingType().equals("mmr") || settingDto.getSettingType().equals("rank")) {
                            mmrDivide(queueName, settingDto.getSettingHeadcount());
                        }
                        else {
                            positionDivide(queueName);
                        }

                        // 팀 정보 제외 전체 삭제
                        delete(queueName);                        
                    }
                    return result;
                }
                Thread.sleep(1000);
            }
            System.out.println("size = "+hashOperations.size("accept:"+queueName));
            if(hashOperations.size("accept:"+queueName) == (settingDto.getSettingHeadcount()*2)) {
                result.put("code", "success");
                    result.put("listname", queueName);
                    if(condition) {
                        if(settingDto.getSettingType().equals("mmr") || settingDto.getSettingType().equals("rank")) {
                            mmrDivide(queueName, settingDto.getSettingHeadcount());
                        }
                        else {
                            positionDivide(queueName);
                        }
                        // 팀 정보 제외 전체 삭제
                        delete(queueName);                        
                    }
                return result;
            }
            // 시간안에 모두 동의하지 않은 경우
            result.put("code", "fail");
            result.put("message", "수락하지 않은 유저가 있습니다. 다시 대기열로 돌아갑니다.");
            Long afterSize = hashOperations.size("accept:"+queueName);
            if(beforeSize==afterSize) {

                Map<Object, Object> userMap = hashOperations.entries("map:"+queueName);

                for(Object key : userMap.keySet()) {
                    if(hashOperations.hasKey("accept:"+queueName, key)==false) {
                        
                        if(settingDto.getSettingType().equals("position") || settingDto.getSettingType().equals("all")) {
                            positionDelete(queueName, key);
                        }
                        // 공통으로 지우는 부분
                        commonDelete(queueName, key);
                    }
                }
                hashOperations.getOperations().delete("accept:"+queueName);

            }

            return result;
        }
    }

    // map, queueAll, acceptTime을 지운다
    private void commonDelete(String queueName, Object key) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        hashOperations.delete("map:"+queueName, key);
        hashOperations.delete("queueAll", key);
        hashOperations.delete("acceptTime", queueName);

    }

    // 포지션 지우기
    private void positionDelete(String queueName, Object key) throws JsonMappingException, JsonProcessingException {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        UserAllDto userAllDto = objectMapper.readValue(hashOperations.get("map:"+queueName, key).toString(), UserAllDto.class);
        String position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();
        
        Object object = hashOperations.get("position:"+queueName, position);
        
        if(object.toString().equals("2")) {
            hashOperations.put("position:"+queueName, position, "1");
        }
        else {
            hashOperations.delete("position:"+queueName, position);
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

    // 매칭 진행 : 큐 사이즈 확인, 정원이 다 차면 재귀 메소드 탈출
    private String queueCheck(Long size, String listName, UserAllDto userAllDto, int headCount) throws InterruptedException, JsonMappingException, JsonProcessingException {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        
        if(hashOperations.hasKey("map:"+listName, Integer.toString(userAllDto.getUserId()))==false) {
            return "cancel";
        }
        else {
            if(size < headCount) {
                System.out.println("와서 뱅글뱅글 도는중 : "+size);
                Thread.sleep(1000);
                String status = queueCheck(hashOperations.size("map:"+listName), listName, userAllDto, headCount);
                if(status.equals("cancel")) {
                    return "cancel";
                }
            }
            return "ok";
        }
    }

    private List<String> rankListAdd(RankDto rankDto) {
        
        List<RankDto> rankDtoList = mainMapper.findByRank();
        
        rankDtoList.get(0);
        List<String> rankList = new ArrayList<>();

        if(rankDto.getRankLevel()==1) {
            rankList.add(rankDtoList.get(1).getRankName());
        }
        else if(rankDto.getRankLevel()==rankDtoList.size()) {
            rankList.add(rankDtoList.get(8).getRankName());
        }
        else {
            rankList.add(rankDtoList.get(rankDto.getRankLevel()-2).getRankName());
            rankList.add(rankDtoList.get(rankDto.getRankLevel()).getRankName());
        }
        rankList.add(rankDto.getRankName());

        return rankList;
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
    private String positionCheck(List<String> positionList, UserAllDto userAllDto, int min, int max, Boolean rankContain) throws JsonProcessingException {

        String rank = "";
        if(rankContain) {
            rank = mainMapper.findByRankId(userAllDto.getRankId()).getRankName();
        }
        String position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String queueName = "";

        for (int i = 0; i < positionList.size(); i++) {
            // 포지션 조건 추가
            if(hashOperations.hasKey("position:"+positionList.get(i), position)) {
                if(Integer.valueOf(hashOperations.get("position:"+positionList.get(i), position).toString())>1) {
                    if(i==positionList.size()-1) {
                        String uuid = UUID.randomUUID().toString();
                        if(rank.equals("")) {
                            queueName = min+"_"+max+"_"+uuid;
                        }
                        else {
                            queueName = rank+"_"+min+"_"+max+"_"+uuid;
                        }
                        queueCreate(queueName, userAllDto);
                        System.out.println("일치하는 mmr이 있으나 포지션이 없어서 큐 새로 생성");
                        hashOperations.put("position:"+queueName, position, "1");
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
                    hashOperations.put("position:"+queueName, position, "2");
                    return queueName;
                }
            }
            else {
                // 포지션 자리가 존재해 기존의 큐에 값 추가, 포지션 자리가 0인경우, 없는 경우
                queueName = positionList.get(i);
                queueCreate(queueName, userAllDto);
                hashOperations.put("position:"+queueName, position, "1");
                return queueName;
            }
        }

        return queueName;

    }

    // 큐를 지우기
    public void delete(String listName) {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        SettingDto settingDto = mainMapper.findBySettingId();

        int size = hashOperations.entries("map:"+listName).size();

        hashOperations.getOperations().delete("map:"+listName);        
        hashOperations.getOperations().delete("accept:"+listName);
        hashOperations.delete("acceptTime", listName);

        if(settingDto.getSettingType().equals("position") || settingDto.getSettingType().equals("all")) {
            hashOperations.getOperations().delete("position:"+listName);
        }

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

    // mmr 팀 나누기 & 랭크 팀 나누기
    public void mmrDivide(String queueName, int headCount) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        Map<Object, Object> map = hashOperations.entries("accept:"+queueName);

        List<UserAllDto> userListA = new ArrayList<>();

        List<UserAllDto> userListB = new ArrayList<>();

        // mmr 나누기
        for(Object key : map.keySet() ){
            UserAllDto user = objectMapper.readValue(map.get(key).toString(), UserAllDto.class);
            userListA.add(user);
        }

        HashMap<Integer, List<List<UserAllDto>>> teamResult = new HashMap<>();

        mmrCombi(teamResult, userListA, userListB, headCount);

        System.out.println("순조로운 진행~~");
        for(Integer key : teamResult.keySet()) {
            for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
                UserAllDto userAllDtoA = teamResult.get(key).get(0).get(i);
                UserAllDto userAllDtoB = teamResult.get(key).get(1).get(i);
                hashOperations.put("teamA:"+queueName, Integer.toString(userAllDtoA.getUserId()), objectMapper.writeValueAsString(userAllDtoA));
                hashOperations.put("teamB:"+queueName, Integer.toString(userAllDtoB.getUserId()), objectMapper.writeValueAsString(userAllDtoB));
            }
            System.out.println("최종값 : " + key);
        }
    }

    private void mmrCombi(HashMap<Integer, List<List<UserAllDto>>> teamResult, List<UserAllDto> userListA, List<UserAllDto> userListB, int count) {
        
        if(count == 0) {
            int sumA = 0;
            int sumB = 0;
            List<UserAllDto> userA = new ArrayList<>();
            List<UserAllDto> userB = new ArrayList<>();

            for (int i = 0; i < userListA.size(); i++) {
                sumA = userListA.get(i).getUserMmr();
                sumB = userListB.get(i).getUserMmr();
                userB.add(userListB.get(i));
                userA.add(userListA.get(i));
            }

            int sumDif = Math.abs(sumA - sumB);
            List<List<UserAllDto>> teamList = new ArrayList<>();
           
            teamList.add(userA);
            teamList.add(userB);

            // System.out.println("합A : " + sumA + " 합B : " + sumB + " 차이 : "+sumDif);

            if(sumDif==0) {
                teamResult.put(sumDif, teamList);
            }
            if(teamResult.size() > 0) {
                for(Integer key : teamResult.keySet() ){
                    if(key > sumDif) {
                        teamResult.remove(key);
                        teamResult.put(sumDif, teamList);
                    }
                }
            }
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
            }
        }
        else {
            for (int i = 0; i < userListA.size(); i++) {
                List<UserAllDto> newUesrListA = new ArrayList<>();
                List<UserAllDto> newUesrListB = new ArrayList<>();
                for (int j = 0; j < userListB.size(); j++) {
                    newUesrListB.add(userListB.get(j));
                }
                for (int j = 0; j < userListA.size(); j++) {
                    if(i < j || i > j) {
                        newUesrListA.add(userListA.get(j));
                    }
                    else {
                        newUesrListB.add(userListA.get(i));
                    }
                }
                mmrCombi(teamResult, newUesrListA, newUesrListB, count-1);
            }
        }
    }

    // 포지션 & 전체 팀 나누기
    public void positionDivide(String queueName) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        List<PositionDto> positionAllList = mainMapper.findByPosition();

        Map<Object, Object> map = hashOperations.entries("accept:"+queueName);

        List<List<UserAllDto>> userPositionList = new ArrayList<>(); 

        // postion이랑 mmr 얻어와서 비교하기
        for (int i = 0; i < positionAllList.size(); i++) {
            for(Object key : map.keySet() ){
                UserAllDto user = objectMapper.readValue(map.get(key).toString(), UserAllDto.class);
                if(user.getPositionId()==positionAllList.get(i).getPositionId()) {
                    if(userPositionList.size() > i) {
                        userPositionList.get(i).add(user);
                    }
                    else {
                        List<UserAllDto> positionList = new ArrayList<>();
                        positionList.add(user);
                        userPositionList.add(positionList);
                    }
                }
            }
        }    
        int positionListSize = userPositionList.size();
        List<UserAllDto> userInfoA = new ArrayList<>();
        List<UserAllDto> userInfoB = new ArrayList<>();

        HashMap<Integer, List<List<UserAllDto>>> teamResult = new HashMap<>();

        // 포지션에 따른 조합
        positionCombi(userInfoA, userInfoB, positionListSize-1, userPositionList, teamResult);

        System.out.println("순조로운 진행~~");
        for(Integer key : teamResult.keySet())
        for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
            UserAllDto userAllDtoA = teamResult.get(key).get(0).get(i);
            UserAllDto userAllDtoB = teamResult.get(key).get(1).get(i);
            hashOperations.put("teamA:"+queueName, Integer.toString(userAllDtoA.getUserId()), objectMapper.writeValueAsString(userAllDtoA));
            hashOperations.put("teamB:"+queueName, Integer.toString(userAllDtoB.getUserId()), objectMapper.writeValueAsString(userAllDtoB));
        }
    }

    private void positionCombi(List<UserAllDto> userInfoA, List<UserAllDto> userInfoB, int count, 
        List<List<UserAllDto>> userPositionList, HashMap<Integer, List<List<UserAllDto>>> teamResult) {
        
        if(count < 0) {
            int sumA = 0;
            int sumB = 0;
            for (int i = 0; i < userInfoA.size(); i++) {
                sumA += userInfoA.get(i).getUserMmr();
                sumB += userInfoB.get(i).getUserMmr();
            }
            int sumDif = Math.abs(sumA - sumB);
            List<List<UserAllDto>> teamList = new ArrayList<>();
            List<UserAllDto> userA = new ArrayList<>();
            List<UserAllDto> userB = new ArrayList<>();
           
            for (int j = 0; j < userInfoA.size(); j++) {
                userA.add(userInfoA.get(j));
                userB.add(userInfoB.get(j));
            }
            teamList.add(userA);
            teamList.add(userB);

            if(sumDif==0) {
                teamResult.put(sumDif, teamList);
            }
            if(teamResult.size() > 0) {
                for(Integer key : teamResult.keySet() ){
                    if(key > sumDif) {
                        teamResult.remove(key);
                        teamResult.put(sumDif, teamList);
                    }
                }
            }
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
            }
            userInfoA.clear();
            userInfoB.clear();
        }
        else {
            for (int i = 0; i < userPositionList.get(count).size(); i++) {
                List<UserAllDto> userA = new ArrayList<>();
                List<UserAllDto> userB = new ArrayList<>();
                for (int j = 0; j < userInfoA.size(); j++) {
                    userA.add(userInfoA.get(j));
                    userB.add(userInfoB.get(j));
                }
                userA.add(userPositionList.get(count).get(i));
                if(i==0) {
                    userB.add(userPositionList.get(count).get(1));
                }
                else {
                    userB.add(userPositionList.get(count).get(0));
                }                
                positionCombi(userA, userB, count-1, userPositionList, teamResult);
            }
        }
    }

}
