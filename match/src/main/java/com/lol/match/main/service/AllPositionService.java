package com.lol.match.main.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.match.common.exception.BusinessLogicException;
import com.lol.match.common.exception.ExceptionCode;
import com.lol.match.main.mapper.MainMapper;
import com.lol.match.main.model.GroupMatchAllDto;
import com.lol.match.main.model.GroupMatchPositionDto;
import com.lol.match.main.model.PositionDto;
import com.lol.match.main.model.RankDto;
import com.lol.match.main.model.SettingDto;
import com.lol.match.main.model.UserAllDto;
import com.lol.match.main.model.UserPositionDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
// @RequiredArgsConstructor
// public class AllPositionService extends CommonService{
public class AllPositionService extends CommonService{

 
    // private final RedisTemplate<String, Object> redisTemplate;

    // private final MainMapper mainMapper;

    // private final ObjectMapper objectMapper;

    // private final CommonService commonService;

    public AllPositionService(RedisTemplate<String, Object> redisTemplate, MainMapper mainMapper, ObjectMapper objectMapper) {
        super(redisTemplate, mainMapper, objectMapper);
    }


    // All(포지션, 랭크, MMR) 매칭 start -----------
    public HashMap<String, String> matchUserAll(int userId) throws Exception {

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingAllId();

        // mmr, rank, position 고려하여 매칭 시켜주는 경우 
        return allIsMap(userId, settingDto);

    }

    private HashMap<String, String> allIsMap(int userId, SettingDto settingDto) throws Exception {

        HashMap<String, String> result = new HashMap<>();
        ListOperations<String, Object> listOperations = redisTemplate.opsForList();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        
        UserAllDto userAllDto = mainMapper.findByAllUserId(userId);

        int mmr = userAllDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("matchAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            RankDto rankdto = mainMapper.findByRankId(userAllDto.getRankId());
            String position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();
    
            String teamName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;
    
            List<String> rankList = new ArrayList<>();
            
            // Redis Data List 출력
            Long listSize = listOperations.size("teamList");
            List<Object> teamList = listOperations.range("teamList", 0, listSize-1);

            // 랭크 계급 설정
            rankList = rankListAdd(rankdto, teamList, settingDto);
    
            List<String> positionList = new ArrayList<>();
    
            // mmr 범위 조정
            for(String fileterList : rankList) {
                String minRange = String.valueOf(fileterList).split("_")[1];
                String maxRange = String.valueOf(fileterList).split("_")[2];
    
                if (Integer.parseInt(minRange) <= mmr) {
                    if (Integer.parseInt(maxRange) >= mmr) {
                        positionList.add(fileterList);
                    }
                }           
            }
    
            if(positionList.size()>0) {
                teamName = allCheck(positionList, userId, min, max);
            }
            else {
                // 일치하는 mmr이 없을 경우
                log.info("새롭게 팀을 추가함 ");
                String uuid = UUID.randomUUID().toString();
                teamName = rankdto.getRankName()+"_"+min+"_"+max+"_"+uuid;
                teamCreate(teamName, userAllDto);
                hashOperations.put("position:"+teamName, position, "1");
                listOperations.rightPush("teamList", teamName);
    
            }
            result = teamAddResult(id, teamName, settingDto);

        }
        return result;
    }

    // 팀에서 유저 정보 삭제 : 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : All & Position
    public HashMap<String, String> teamListDeleteUserAllPosition(int userId) throws Exception {

        HashMap<String, String> result = new HashMap<>();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        String id = Integer.toString(userId);
        if(hashOperations.hasKey("matchAll", id)) {
            Object key = hashOperations.get("matchAll", id);

            UserAllDto user = objectMapper.readValue(hashOperations.get("match:"+key.toString(), id).toString(), UserAllDto.class);

            String position = mainMapper.findByPositionId(user.getPositionId()).getPositionName();

            // 포지션 수 줄임
            Object userPosition = hashOperations.get("position:"+key.toString(), position);
    
            if(Integer.parseInt(userPosition.toString())==1) {
                hashOperations.delete("position:"+key.toString(), position);
            }
            else if(Integer.parseInt(userPosition.toString())==2) {
                hashOperations.put("position:"+key.toString(), userPosition, "1");
            }

            // 전체 키에서 삭제
            hashOperations.delete("matchAll", id);

            // 기존 리스트에서 삭제
            hashOperations.delete("match:"+key.toString(), id);
    
            result.put("code", "success");
        }
        else {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }

        return result;
    }

    // 대전 매칭 완료하기 
    public HashMap<String, String> matchAcceptUserAll(int userId) throws Exception {
    
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        boolean condition = true;

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingAllId();

        String id = Integer.toString(userId);
        
        // 요청 검증
        if(hashOperations.hasKey("matchAll", id)==false) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        String teamName = hashOperations.get("matchAll", id).toString();

        if(hashOperations.size("match:"+teamName) < (settingDto.getSettingHeadcount()*2) 
            || hashOperations.size("match:"+teamName) > (settingDto.getSettingHeadcount()*2)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        // 두번 요청했을때 예외 처리
        if(hashOperations.hasKey("accept:"+teamName, id)==true) {
            throw new BusinessLogicException(ExceptionCode.DUPLICATION_REQUEST);
        }
        Date date = new Date();

        Object object = hashOperations.get("acceptTime", teamName);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

        Date saveDate = simpleDateFormat.parse(object.toString());

        if(saveDate.after(date)==false) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String userAll = hashOperations.get("match:"+teamName, id).toString();
            hashOperations.put("accept:"+teamName, id, userAll);
            Long beforeSize = hashOperations.size("accept:"+teamName);
            
            while(saveDate.after(date)) {
                
                Date newDate = new Date();
                // log.info("결과 : "+saveDate.after(newDate));
                
                // 설정한 시간이 지날때까지 사이즈 검토
                if(saveDate.after(newDate)==false) {
                    condition = false;
                    break;
                }
                if(hashOperations.size("accept:"+teamName) < (settingDto.getSettingHeadcount()*2)) {
                    condition = false;
                    Thread.sleep(1000);
                    continue;
                }
                // 설정한 시간이 안지났지만 정원이 다 차면 미리 바로 탈출
                if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                    result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        allDivide(teamName, settingDto.getSettingHeadcount());

                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoAllPosition(teamName);                        
                    }
                    return result;
                }
            }
            // log.info("size = "+hashOperations.size("accept:"+teamName));
            if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        allDivide(teamName, settingDto.getSettingHeadcount());
                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoAllPosition(teamName);                        
                    }
                return result;
            }
            // 시간안에 모두 동의하지 않은 경우
            result.put("code", "fail");
            result.put("message", "수락하지 않은 유저가 있습니다. 다시 대기열로 돌아갑니다.");
            Long afterSize = hashOperations.size("accept:"+teamName);
            // 마지막에 들어온 사람이 모든 키 삭제
            if(beforeSize==afterSize) {

                Map<Object, Object> userMap = hashOperations.entries("match:"+teamName);

                for(Object key : userMap.keySet()) {
                    if(hashOperations.hasKey("accept:"+teamName, key)==false) {
                        positionDelete(teamName, key);
                        // 공통으로 지우는 부분
                        commonDelete(teamName, key);
                    }
                }
                hashOperations.getOperations().delete("accept:"+teamName);
                hashOperations.delete("acceptTime", teamName);
            }

            return result;
        }
    }

    // 전체 팀 나누기
    public void allDivide(String teamName, int headCount) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        List<PositionDto> positionAllList = mainMapper.findByPosition();

        Map<Object, Object> acceptMap = hashOperations.entries("accept:"+teamName);

        List<List<UserAllDto>> userPositionList = new ArrayList<>(); 

        // postion이랑 mmr 얻어와서 비교하기
        for (int i = 0; i < positionAllList.size(); i++) {
            for(Object key : acceptMap.keySet()){
                UserAllDto user = objectMapper.readValue(acceptMap.get(key).toString(), UserAllDto.class);
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
        List<UserAllDto> userInfoA = new ArrayList<>();

        HashMap<Integer, List<List<UserAllDto>>> teamResult = new HashMap<>();

        // A팀 - B팀의 값이 최솟값일 경우 팀 매칭
        userInfoA.add(userPositionList.get(0).get(0));
        allCombi(userInfoA, 1, userPositionList, teamResult, headCount);

        for(Integer key : teamResult.keySet()) {
            for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
                UserAllDto userAllDtoA = teamResult.get(key).get(0).get(i);
                UserAllDto userAllDtoB = teamResult.get(key).get(1).get(i);
                hashOperations.put("teamA:"+teamName, Integer.toString(userAllDtoA.getUserId()), objectMapper.writeValueAsString(userAllDtoA));
                hashOperations.put("teamB:"+teamName, Integer.toString(userAllDtoB.getUserId()), objectMapper.writeValueAsString(userAllDtoB));
            }
        }
    }

    private void allCombi(List<UserAllDto> userInfoA, int count, 
        List<List<UserAllDto>> userPositionList, HashMap<Integer, List<List<UserAllDto>>> teamResult, int headCount) {
        
        if(count == headCount) {
            List<UserAllDto> userInfoB = new ArrayList<>();

            for (int i = 0; i < userInfoA.size(); i++) {
                for (int j = 0; j < userPositionList.get(i).size(); j++) {
                    if(userInfoA.contains(userPositionList.get(i).get(j))==false) {
                        userInfoB.add(userPositionList.get(i).get(j));
                    }
                }
            }

            int sumA = 0;
            int sumB = 0;
            List<List<UserAllDto>> teamList = new ArrayList<>();
            if(userInfoA.size()!=userInfoB.size()) {
                log.info("에러 리스트 확인 : A 리스트 : {}, B 리스트 : {}", userInfoA.toString(), userInfoB.toString());
                throw new BusinessLogicException(ExceptionCode.SERVER_ERROR);
            }
            for (int i = 0; i < headCount; i++) {
                sumA += userInfoA.get(i).getUserMmr();
                sumB += userInfoB.get(i).getUserMmr();
            }
            int sumDif = Math.abs(sumA - sumB);
        
            teamList.add(userInfoA);
            teamList.add(userInfoB);

            log.info("합A : " + sumA + " 합B : " + sumB + " 차이 : "+sumDif);
            
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
            }
            else {
                for(Integer key : teamResult.keySet()){
                    if(key > sumDif) {
                        teamResult.clear();
                        teamResult.put(sumDif, teamList);
                    }
                }
            }
        }
        else {
            for (int i = 0; i < userPositionList.get(count).size(); i++) {
                List<UserAllDto> userA = new ArrayList<>();

                for (int j = 0; j < userInfoA.size(); j++) {
                    userA.add(userInfoA.get(j));
                }
                userA.add(userPositionList.get(count).get(i));

                allCombi(userA, count+1, userPositionList, teamResult, headCount);
                if(teamResult.containsKey(0)) {
                    return;
                }
            }
        }
    }

    // 팀 배정 정보 및 본인이 속한 팀 정보 주기
    public GroupMatchAllDto matchCompleteUserAll(int userId, String teamName) throws Exception {
    
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        String id = Integer.toString(userId);

        String userInfo;

        if(hashOperations.hasKey("teamA:"+teamName, id)) {
            userInfo = "A";
        }
        else if(hashOperations.hasKey("teamB:"+teamName, id)){
            userInfo = "B";
        }
        else {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        Map<Object, Object> teamAMap = hashOperations.entries("teamA:"+teamName);
        Map<Object, Object> teamBMap = hashOperations.entries("teamB:"+teamName);

        GroupMatchAllDto groupMatchDto = new GroupMatchAllDto(userInfo, teamInfoAll(teamAMap), teamInfoAll(teamBMap));
        
        return groupMatchDto;

    }

    // 팀 정보 List 형태로 저장
    private List<UserAllDto> teamInfoAll(Map<Object, Object> teamMap) throws JsonMappingException, JsonProcessingException {
        int count = 0;

        List<UserAllDto> teamList = new ArrayList<>();

        for(Object key : teamMap.keySet()) {
            UserAllDto userAllDto = objectMapper.readValue(teamMap.get(key).toString(), UserAllDto.class);
            teamList.add(count, userAllDto);
            count += 1;
        }

        return teamList;
    }

    // All(포지션, 랭크, MMR) 매칭 end -----------


    // Position(Position, MMR) 매칭 start -----------

    public HashMap<String, String> matchUserPosition(int userId) throws Exception {

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingPositionId();

        // mmr, position 고려하여 매칭 시켜주는 경우
        return positionIsMap(userId, settingDto);

    }

    private HashMap<String, String> positionIsMap(int userId, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();

        ListOperations<String, Object> listOperations = redisTemplate.opsForList();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        UserPositionDto userPositionDto = mainMapper.findByPositionUserId(userId);

        int mmr = userPositionDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("matchAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String teamName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;

            String position = mainMapper.findByPositionId(userPositionDto.getPositionId()).getPositionName();

            List<String> positionList = new ArrayList<>();

            Long listSize = listOperations.size("teamList");
            List<Object> teamList = listOperations.range("teamList", 0, listSize-1);

            for(Object key : teamList) {
                if(hashOperations.size("match:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("match:"+key.toString()) > 0) {

                    String minRange = key.toString().split("_")[1];
                    String maxRange = key.toString().split("_")[2];
        
                    if (Integer.parseInt(minRange) <= mmr) {
                        if (Integer.parseInt(maxRange) >= mmr) {
                            positionList.add(key.toString());
                        }
                    }
                }           
            }
            if(positionList.size()>0) {
                teamName = positionCheck(positionList, userId, min, max);
            }
            else {
                String uuid = UUID.randomUUID().toString();
                teamName = min+"_"+max+"_"+uuid;
                teamCreate(teamName, userPositionDto);
                hashOperations.put("position:"+teamName, position, "1");
                listOperations.rightPush("teamList", teamName);
            }

            result = teamAddResult(id, teamName, settingDto);
        }

        return result;
    }

    // 대전 매칭 완료하기 
    public HashMap<String, String> matchAcceptUserPosition(int userId) throws Exception {
    
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        boolean condition = true;

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingPositionId();

        String id = Integer.toString(userId);
        
        // 요청 검증
        if(hashOperations.hasKey("matchAll", id)==false) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        String teamName = hashOperations.get("matchAll", id).toString();

        if(hashOperations.size("match:"+teamName) < (settingDto.getSettingHeadcount()*2) 
            || hashOperations.size("match:"+teamName) > (settingDto.getSettingHeadcount()*2)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        // 두번 요청했을때 예외 처리
        if(hashOperations.hasKey("accept:"+teamName, id)==true) {
            throw new BusinessLogicException(ExceptionCode.DUPLICATION_REQUEST);
        }
        Date date = new Date();

        Object object = hashOperations.get("acceptTime", teamName);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 

        Date saveDate = simpleDateFormat.parse(object.toString());

        if(saveDate.after(date)==false) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String userAll = hashOperations.get("match:"+teamName, id).toString();
            hashOperations.put("accept:"+teamName, id, userAll);
            Long beforeSize = hashOperations.size("accept:"+teamName);
            
            while(saveDate.after(date)) {
                
                Date newDate = new Date();
                // log.info("결과 : "+saveDate.after(newDate));
                
                // 설정한 시간이 지날때까지 사이즈 검토
                if(saveDate.after(newDate)==false) {
                    condition = false;
                    break;
                }
                if(hashOperations.size("accept:"+teamName) < (settingDto.getSettingHeadcount()*2)) {
                    condition = false;
                    Thread.sleep(1000);
                    continue;
                }
                // 설정한 시간이 안지났지만 정원이 다 차면 미리 바로 탈출
                if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                    result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        positionDivide(teamName, settingDto.getSettingHeadcount());

                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoAllPosition(teamName);                        
                    }
                    return result;
                }
            }
            // log.info("size = "+hashOperations.size("accept:"+teamName));
            if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        positionDivide(teamName, settingDto.getSettingHeadcount());
                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoAllPosition(teamName);                        
                    }
                return result;
            }
            // 시간안에 모두 동의하지 않은 경우
            result.put("code", "fail");
            result.put("message", "수락하지 않은 유저가 있습니다. 다시 대기열로 돌아갑니다.");
            Long afterSize = hashOperations.size("accept:"+teamName);
            if(beforeSize==afterSize) {

                Map<Object, Object> userMap = hashOperations.entries("match:"+teamName);

                for(Object key : userMap.keySet()) {
                    if(hashOperations.hasKey("accept:"+teamName, key)==false) {
                        positionDelete(teamName, key);
                        // 공통으로 지우는 부분
                        commonDelete(teamName, key);
                    }
                }
                hashOperations.getOperations().delete("accept:"+teamName);
                hashOperations.delete("acceptTime", teamName);
            }

            return result;
        }
    }

        // 포지션 & 전체 팀 나누기
        public void positionDivide(String teamName, int headCount) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        List<PositionDto> positionAllList = mainMapper.findByPosition();

        Map<Object, Object> acceptMap = hashOperations.entries("accept:"+teamName);

        List<List<UserPositionDto>> userPositionList = new ArrayList<>(); 

        // postion이랑 mmr 얻어와서 비교하기
        for (int i = 0; i < positionAllList.size(); i++) {
            for(Object key : acceptMap.keySet()){
                UserPositionDto user = objectMapper.readValue(acceptMap.get(key).toString(), UserPositionDto.class);
                if(user.getPositionId()==positionAllList.get(i).getPositionId()) {
                    if(userPositionList.size() > i) {
                        userPositionList.get(i).add(user);
                    }
                    else {
                        List<UserPositionDto> positionList = new ArrayList<>();
                        positionList.add(user);
                        userPositionList.add(positionList);
                    }
                }
            }
        }    
        List<UserPositionDto> userInfoA = new ArrayList<>();

        HashMap<Integer, List<List<UserPositionDto>>> teamResult = new HashMap<>();

        // A팀 - B팀의 값이 최솟값일 경우 팀 매칭
        userInfoA.add(userPositionList.get(0).get(0));
        positionCombi(userInfoA, 1, userPositionList, teamResult, headCount);

        for(Integer key : teamResult.keySet()) {
            for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
                UserPositionDto userPositionDtoA = teamResult.get(key).get(0).get(i);
                UserPositionDto userPositionDtoB = teamResult.get(key).get(1).get(i);
                hashOperations.put("teamA:"+teamName, Integer.toString(userPositionDtoA.getUserId()), objectMapper.writeValueAsString(userPositionDtoA));
                hashOperations.put("teamB:"+teamName, Integer.toString(userPositionDtoB.getUserId()), objectMapper.writeValueAsString(userPositionDtoB));
            }
        }
    }

    private void positionCombi(List<UserPositionDto> userInfoA, int count, 
        List<List<UserPositionDto>> userPositionList, HashMap<Integer, List<List<UserPositionDto>>> teamResult, int headCount) {
        
        if(count == headCount) {
            List<UserPositionDto> userInfoB = new ArrayList<>();

            for (int i = 0; i < userInfoA.size(); i++) {
                for (int j = 0; j < userPositionList.get(i).size(); j++) {
                    if(userInfoA.contains(userPositionList.get(i).get(j))==false) {
                        userInfoB.add(userPositionList.get(i).get(j));
                    }
                }
            }

            int sumA = 0;
            int sumB = 0;
            List<List<UserPositionDto>> teamList = new ArrayList<>();
            if(userInfoA.size()!=userInfoB.size()) {
                log.info("에러 리스트 확인 : A 리스트 : {}, B 리스트 : {}", userInfoA.toString(), userInfoB.toString());
                throw new BusinessLogicException(ExceptionCode.SERVER_ERROR);
            }
            for (int i = 0; i < headCount; i++) {
                sumA += userInfoA.get(i).getUserMmr();
                sumB += userInfoB.get(i).getUserMmr();
            }
            int sumDif = Math.abs(sumA - sumB);
        
            teamList.add(userInfoA);
            teamList.add(userInfoB);

            log.info("합A : " + sumA + " 합B : " + sumB + " 차이 : "+sumDif);
            
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
            }
            else {
                for(Integer key : teamResult.keySet()){
                    if(key > sumDif) {
                        teamResult.clear();
                        teamResult.put(sumDif, teamList);
                    }
                }
            }
        }
        else {
            for (int i = 0; i < userPositionList.get(count).size(); i++) {
                List<UserPositionDto> userA = new ArrayList<>();

                for (int j = 0; j < userInfoA.size(); j++) {
                    userA.add(userInfoA.get(j));
                }
                userA.add(userPositionList.get(count).get(i));

                positionCombi(userA, count+1, userPositionList, teamResult, headCount);
                if(teamResult.containsKey(0)) {
                    return;
                }
            }
        }
    }

    // 팀 배정 정보 및 본인이 속한 팀 정보 주기
    public GroupMatchPositionDto matchCompleteUserPosition(int userId, String teamName) throws Exception {
    
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        String id = Integer.toString(userId);

        String userInfo;

        if(hashOperations.hasKey("teamA:"+teamName, id)) {
            userInfo = "A";
        }
        else if(hashOperations.hasKey("teamB:"+teamName, id)){
            userInfo = "B";
        }
        else {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        Map<Object, Object> teamAMap = hashOperations.entries("teamA:"+teamName);
        Map<Object, Object> teamBMap = hashOperations.entries("teamB:"+teamName);

        GroupMatchPositionDto groupMatchDto = new GroupMatchPositionDto(userInfo, teamInfoPosition(teamAMap), teamInfoPosition(teamBMap));
        
        return groupMatchDto;

    }

    // 팀 정보 List 형태로 저장
    private List<UserPositionDto> teamInfoPosition(Map<Object, Object> teamMap) throws JsonMappingException, JsonProcessingException {
        int count = 0;

        List<UserPositionDto> teamList = new ArrayList<>();

        for(Object key : teamMap.keySet()) {
            UserPositionDto userPositionDto = objectMapper.readValue(teamMap.get(key).toString(), UserPositionDto.class);
            teamList.add(count, userPositionDto);
            count += 1;
        }

        return teamList;
    }
    
    // Position(Position, MMR) 매칭 end -----------
    

}
