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
import com.lol.match.main.model.GroupMatchRankDto;
import com.lol.match.main.mapper.MainMapper;
import com.lol.match.main.model.GroupMatchMmrDto;
import com.lol.match.main.model.RankDto;
import com.lol.match.main.model.SettingDto;
import com.lol.match.main.model.UserMmrDto;
import com.lol.match.main.model.UserRankDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
// @RequiredArgsConstructor
public class MmrRankService extends CommonService{
    
    // private final RedisTemplate<String, Object> redisTemplate;

    // private final MainMapper mainMapper;

    // private final ObjectMapper objectMapper;

    // private final CommonService commonService;

    public MmrRankService(RedisTemplate<String, Object> redisTemplate, MainMapper mainMapper, ObjectMapper objectMapper) {
        super(redisTemplate, mainMapper, objectMapper);
    }
    
    // MMR 매칭 start -----------
    public HashMap<String, String> matchUserMmr(int userId) throws Exception {

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingMmrId();

        // mmr만 고려하여 매칭 시켜주는 경우 
        return mmrIsMap(userId, settingDto);

    }

    private HashMap<String, String> mmrIsMap(int userId, SettingDto settingDto) throws JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();

        ListOperations<String, Object> listOperations = redisTemplate.opsForList();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        UserMmrDto userMmrDto = mainMapper.findByMmrUserId(userId);

        int mmr = userMmrDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("matchAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String teamName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;

            Long listSize = listOperations.size("teamList");
            List<Object> teamList = listOperations.range("teamList", 0, listSize-1);

            for(Object key : teamList) {
                
                if(hashOperations.size("match:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("match:"+key.toString()) > 0) {
                    String minRange = key.toString().split("_")[0];
                    String maxRange = key.toString().split("_")[1];
        
                    if (Integer.parseInt(minRange) <= mmr) {
                        if (Integer.parseInt(maxRange) >= mmr) {
                            teamName = key.toString();
                            teamCreate(teamName, userMmrDto);
                            break;
                        }
                    }
                }           
            }
            if(teamName.equals("")) {
                String uuid = UUID.randomUUID().toString();
                teamName = min+"_"+max+"_"+uuid;
                teamCreate(teamName, userMmrDto);
                listOperations.rightPush("teamList", teamName);
            }
            result = teamAddResult(id, teamName, settingDto);

        }
        return result;
    }

    // 팀에서 유저 정보 삭제 : 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : Mmr & Rank
    public HashMap<String, String> teamListDeleteUserMmrRank(int userId) throws Exception {

        HashMap<String, String> result = new HashMap<>();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        String id = Integer.toString(userId);
        if(hashOperations.hasKey("matchAll", id)) {
            Object key = hashOperations.get("matchAll", id);

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
    public HashMap<String, String> matchAcceptUserMmr(int userId) throws Exception {
        
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        boolean condition = true;

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingMmrId();

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
                
                // 설정한 시간이 지날때까지 사이즈 검토, 시간이 지나면 중지
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
                        mmrDivide(teamName, settingDto.getSettingHeadcount());

                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoMmrRank(teamName);                        
                    }
                    return result;
                }
            }
            // log.info("size = "+hashOperations.size("accept:"+teamName));
            if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        mmrDivide(teamName, settingDto.getSettingHeadcount());
                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoMmrRank(teamName);                        
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

    // mmr 팀 나누기 & 랭크 팀 나누기
    public void mmrDivide(String teamName, int headCount) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        Map<Object, Object> map = hashOperations.entries("accept:"+teamName);

        List<UserMmrDto> userInfoA = new ArrayList<>();

        List<UserMmrDto> userListAll = new ArrayList<>();

        // mmr 나누기
        for(Object key : map.keySet() ){
            UserMmrDto user = objectMapper.readValue(map.get(key).toString(), UserMmrDto.class);
            userListAll.add(user);
        }

        userInfoA.add(userListAll.get(0));
        HashMap<Integer, List<List<UserMmrDto>>> teamResult = new HashMap<>();

        // A팀 - B팀의 값이 최솟값일 경우 팀 매칭
        mmrCombi(teamResult, userListAll, userInfoA, headCount, 1);
        
        for(Integer key : teamResult.keySet()) {
            for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
                UserMmrDto userMmrDtoA = teamResult.get(key).get(0).get(i);
                UserMmrDto userMmrDtoB = teamResult.get(key).get(1).get(i);
                hashOperations.put("teamA:"+teamName, Integer.toString(userMmrDtoA.getUserId()), objectMapper.writeValueAsString(userMmrDtoA));
                hashOperations.put("teamB:"+teamName, Integer.toString(userMmrDtoB.getUserId()), objectMapper.writeValueAsString(userMmrDtoB));
            }
            log.info("최종값 : " + key);
        }
    }

    private void mmrCombi(HashMap<Integer, List<List<UserMmrDto>>> teamResult, List<UserMmrDto> userListAll, List<UserMmrDto> userInfoA, int headCount, int count) {    

        if(userInfoA.size() == headCount) {
            List<UserMmrDto> userInfoB = new ArrayList<>();
            
            for (int j = 0; j < userListAll.size(); j++) {
                if(userInfoA.contains(userListAll.get(j))==false) {
                    userInfoB.add(userListAll.get(j));
                }
            }
            int sumA = 0;
            int sumB = 0;
            List<List<UserMmrDto>> teamList = new ArrayList<>();

            for (int j = 0; j < userInfoA.size(); j++) {
                sumA += userInfoA.get(j).getUserMmr();
                sumB += userInfoB.get(j).getUserMmr();
            }
            int sumDif = Math.abs(sumA - sumB);

            log.info("합A : " + sumA + " 합B : " + sumB + " 차이 : "+sumDif);
            teamList.add(userInfoA);
            teamList.add(userInfoB);
            
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
            }
            else {
                for(Integer key : teamResult.keySet()){
                    if(key > sumDif) {
                        teamResult.remove(key);
                        teamResult.put(sumDif, teamList);
                    }
                }
            } 
        }
        else {
            for (int i = count; i < userListAll.size(); i++) {
                List<UserMmrDto> userA = new ArrayList<>();

                for (int j = 0; j < userInfoA.size(); j++) {
                    userA.add(userInfoA.get(j));
                }
                userA.add(userListAll.get(i));

                mmrCombi(teamResult, userListAll, userA, headCount, i+1);
                if(teamResult.containsKey(0)) {
                    return;
                }
            }
        }
    }

    // 팀 배정 정보 및 본인이 속한 팀 정보 주기
    public GroupMatchMmrDto matchCompleteUserMmr(int userId, String teamName) throws Exception {
    
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

        GroupMatchMmrDto groupMatchDto = new GroupMatchMmrDto(userInfo, teamInfoMmr(teamAMap), teamInfoMmr(teamBMap));
        
        return groupMatchDto;

    }

    // 팀 정보 List 형태로 저장
    private List<UserMmrDto> teamInfoMmr(Map<Object, Object> teamMap) throws JsonMappingException, JsonProcessingException {
        int count = 0;

        List<UserMmrDto> teamList = new ArrayList<>();

        for(Object key : teamMap.keySet()) {
            UserMmrDto userMmrDto = objectMapper.readValue(teamMap.get(key).toString(), UserMmrDto.class);
            teamList.add(count, userMmrDto);
            count += 1;
        }

        return teamList;
    }

    // MMR 매칭 end -----------


     // Rank(Rank, MMR) 매칭 start -----------
     public HashMap<String, String> matchUserRank(int userId) throws Exception {

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingRankId();

        // mmr, rank 매칭 시켜주는 경우 
        return rankIsMap(userId, settingDto);

    }

    private HashMap<String, String> rankIsMap(int userId, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();
        
        ListOperations<String, Object> listOperations = redisTemplate.opsForList();

        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        UserRankDto userRankDto = mainMapper.findByRankUserId(userId);

        int mmr = userRankDto.getUserMmr();

        String id = Integer.toString(userId);

        if(hashOperations.hasKey("matchAll", id)) {
            throw new BusinessLogicException(ExceptionCode.BAD_REQUEST);
        }
        else {
            String teamName = "";
            int range = settingDto.getSettingMmr();
            int min = mmr - range > 0 ? mmr - range : 0;
            int max = mmr + range;
    
            RankDto rankdto = mainMapper.findByRankId(userRankDto.getRankId());

            Long listSize = listOperations.size("teamList");
            List<Object> teamList = listOperations.range("teamList", 0, listSize-1);

            // 랭크 계급 설정 및 랭크 필터링
            List<String> rankList = rankListAdd(rankdto, teamList, settingDto);
    
            // mmr 범위 조정
            for(String fileterList : rankList) {
                String minRange = String.valueOf(fileterList).split("_")[1];
                String maxRange = String.valueOf(fileterList).split("_")[2];
    
                if (Integer.parseInt(minRange) <= mmr) {
                    if (Integer.parseInt(maxRange) >= mmr) {
                        teamName = fileterList;
                        teamCreate(teamName, userRankDto);
                        break;
                    }
                }           
            }

            if(teamName.equals("")) {
                String uuid = UUID.randomUUID().toString();
                teamName = rankdto.getRankName()+"_"+min+"_"+max+"_"+uuid;
                teamCreate(teamName, userRankDto);
                listOperations.rightPush("teamList", teamName);
            }
            result = teamAddResult(id, teamName, settingDto);

        }
        return result;
    }

    // 대전 매칭 완료하기 
    public HashMap<String, String> matchAcceptUserRank(int userId) throws Exception {
    
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        boolean condition = true;

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingRankId();

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
                        rankDivide(teamName, settingDto.getSettingHeadcount());

                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoMmrRank(teamName);                        
                    }
                    return result;
                }
            }
            // log.info("size = "+hashOperations.size("accept:"+teamName));
            if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        rankDivide(teamName, settingDto.getSettingHeadcount());
                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfoMmrRank(teamName);                        
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

    // mmr 팀 나누기 & 랭크 팀 나누기
    public void rankDivide(String teamName, int headCount) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        Map<Object, Object> map = hashOperations.entries("accept:"+teamName);

        List<UserRankDto> userInfoA = new ArrayList<>();

        List<UserRankDto> userListAll = new ArrayList<>();

        // mmr 나누기
        for(Object key : map.keySet() ){
            UserRankDto user = objectMapper.readValue(map.get(key).toString(), UserRankDto.class);
            userListAll.add(user);
        }

        userInfoA.add(userListAll.get(0));
        HashMap<Integer, List<List<UserRankDto>>> teamResult = new HashMap<>();

        // A팀 - B팀의 값이 최솟값일 경우 팀 매칭
        rankCombi(teamResult, userListAll, userInfoA, headCount, 1);
        
        for(Integer key : teamResult.keySet()) {
            for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
                UserRankDto userRankDtoA = teamResult.get(key).get(0).get(i);
                UserRankDto userRankDtoB = teamResult.get(key).get(1).get(i);
                hashOperations.put("teamA:"+teamName, Integer.toString(userRankDtoA.getUserId()), objectMapper.writeValueAsString(userRankDtoA));
                hashOperations.put("teamB:"+teamName, Integer.toString(userRankDtoB.getUserId()), objectMapper.writeValueAsString(userRankDtoB));
            }
            log.info("최종값 : " + key);
        }
    }

    private void rankCombi(HashMap<Integer, List<List<UserRankDto>>> teamResult, List<UserRankDto> userListAll, List<UserRankDto> userInfoA, int headCount, int count) {    

        if(userInfoA.size() == headCount) {
            List<UserRankDto> userInfoB = new ArrayList<>();
            
            for (int j = 0; j < userListAll.size(); j++) {
                if(userInfoA.contains(userListAll.get(j))==false) {
                    userInfoB.add(userListAll.get(j));
                }
            }
            int sumA = 0;
            int sumB = 0;
            List<List<UserRankDto>> teamList = new ArrayList<>();

            for (int j = 0; j < userInfoA.size(); j++) {
                sumA += userInfoA.get(j).getUserMmr();
                sumB += userInfoB.get(j).getUserMmr();
            }
            int sumDif = Math.abs(sumA - sumB);

            log.info("합A : " + sumA + " 합B : " + sumB + " 차이 : "+sumDif);
            teamList.add(userInfoA);
            teamList.add(userInfoB);
            
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
            }
            else {
                for(Integer key : teamResult.keySet()){
                    if(key > sumDif) {
                        teamResult.remove(key);
                        teamResult.put(sumDif, teamList);
                    }
                }
            } 
        }
        else {
            for (int i = count; i < userListAll.size(); i++) {
                List<UserRankDto> userA = new ArrayList<>();

                for (int j = 0; j < userInfoA.size(); j++) {
                    userA.add(userInfoA.get(j));
                }
                userA.add(userListAll.get(i));

                rankCombi(teamResult, userListAll, userA, headCount, i+1);
                if(teamResult.containsKey(0)) {
                    return;
                }
            }
        }
    }

    // 팀 배정 정보 및 본인이 속한 팀 정보 주기
    public GroupMatchRankDto matchCompleteUserRank(int userId, String teamName) throws Exception {
    
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

        GroupMatchRankDto groupMatchDto = new GroupMatchRankDto(userInfo, teamInfoRank(teamAMap), teamInfoRank(teamBMap));
        
        return groupMatchDto;

    }

    // 팀 정보 List 형태로 저장
    private List<UserRankDto> teamInfoRank(Map<Object, Object> teamMap) throws JsonMappingException, JsonProcessingException {
        int count = 0;

        List<UserRankDto> teamList = new ArrayList<>();

        for(Object key : teamMap.keySet()) {
            UserRankDto userRankDto = objectMapper.readValue(teamMap.get(key).toString(), UserRankDto.class);
            teamList.add(count, userRankDto);
            count += 1;
        }
        return teamList;
    }

    // Rank(Rank, MMR) 매칭 end -----------

    // mmr team 생성
    private void teamCreate(String teamName, UserMmrDto userMmrDto) throws JsonProcessingException {
        String id = Integer.toString(userMmrDto.getUserId());
        
        userMmrDto.setTeamName(teamName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userMmrDto);
        hashOperations.put("match:"+teamName, id, user);
    }

    // rank team 생성
    private void teamCreate(String teamName, UserRankDto userRankDto) throws JsonProcessingException {
        String id = Integer.toString(userRankDto.getUserId());
        
        userRankDto.setTeamName(teamName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userRankDto);
        hashOperations.put("match:"+teamName, id, user);
    }
}

