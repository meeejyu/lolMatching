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
import com.lol.match.main.model.UserMmrDto;
import com.lol.match.main.model.UserPositionDto;
import com.lol.match.main.model.UserRankDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MainService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final MainMapper mainMapper;

    private final ObjectMapper objectMapper;
 
    public HashMap<String, String> matchUser(int userId) throws Exception {

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingId();

        // TODO : 아래의 4가지 선택지에서 원하는 메소드를 주석 해제 후 사용
        
        // mmr만 고려하여 매칭 시켜주는 경우 
        return mmrIsMap(userId, settingDto);

        // mmr, rank, position 고려하여 매칭 시켜주는 경우 
        // return allIsMap(userId, settingDto);

        // mmr, rank 매칭 시켜주는 경우 
        // return rankIsMap(userId, settingDto);

        // mmr, position 고려하여 매칭 시켜주는 경우
        // return positionIsMap(userId, settingDto);

    }

    private HashMap<String, String> mmrIsMap(int userId, SettingDto settingDto) throws JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

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

            Long listSize = operations.opsForList().size("teamList");
            List<Object> teamList = operations.opsForList().range("teamList", 0, listSize-1);

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
                redisTemplate.opsForList().rightPush("teamList", teamName);
            }
            result = teamAddResult(id, teamName, settingDto);

        }
        return result;
    }

    private HashMap<String, String> teamAddResult(String id, String teamName, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
        HashMap<String, String> result = new HashMap<>();
        boolean condition = true;

        int count = 0;
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();
        hashOperations.put("matchAll", id, teamName);

        // match size가 정원보다 작을때는 계속 머무르기
        if(hashOperations.size("match:"+teamName) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("match:"+teamName) > 0) {
            condition = false;

            // 중도 취소 여부 확인
            String status = teamCheck(hashOperations.size("match:"+teamName), teamName, id, (settingDto.getSettingHeadcount()*2), count);
            if(status.contains("cancel")) {
                result.put("code", status);
                return result;
            }

        }
        result.put("code", "success");
        result.put("listname", teamName);    
        if(condition) {
            // 매칭 동의 시간 추가
            acceptTime(teamName, settingDto.getSettingTime());
        }

        return result;
    }

    private HashMap<String, String> positionIsMap(int userId, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

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

            Long listSize = operations.opsForList().size("teamList");
            List<Object> teamList = operations.opsForList().range("teamList", 0, listSize-1);

            for(Object key : teamList) {
                if(hashOperations.size("match:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("match:"+key.toString()) > 0) {

                    String minRange = key.toString().split("_")[0];
                    String maxRange = key.toString().split("_")[1];
        
                    if (Integer.parseInt(minRange) <= mmr) {
                        if (Integer.parseInt(maxRange) >= mmr) {
                            positionList.add(key.toString());
                        }
                    }
                }           
            }
            if(positionList.size()>0) {
                teamName = positionCheck(positionList, userId, min, max, false);
            }
            else {
                String uuid = UUID.randomUUID().toString();
                teamName = min+"_"+max+"_"+uuid;
                teamCreate(teamName, userPositionDto);
                hashOperations.put("position:"+teamName, position, "1");
                redisTemplate.opsForList().rightPush("teamList", teamName);
            }

            result = teamAddResult(id, teamName, settingDto);
        }

        return result;
    }

    private HashMap<String, String> rankIsMap(int userId, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {

        HashMap<String, String> result = new HashMap<>();
        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();

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

            Long listSize = operations.opsForList().size("teamList");
            List<Object> teamList = operations.opsForList().range("teamList", 0, listSize-1);

            // 랭크 계급 설정 및 랭크 필터링
            List<String> rankList = rankListAdd(rankdto, teamList, settingDto);
            // List<String> rankFilterList = new ArrayList<>();
    
            // rankList = 

            // // 랭크 필터링
            // for (Object key : teamList) {
            //     // 팀 사이즈 확인 
            //     if(hashOperations.size("match:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("match:"+key.toString()) > 0) {
            //         String name = String.valueOf(key).split("_")[0];
            //         for (int i = 0; i < rankList.size(); i++) {
            //             if(name.equals(rankList.get(i))) {
            //                 rankFilterList.add(key.toString());
            //             }
            //         }
            //     }
            // }    
    
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
                redisTemplate.opsForList().rightPush("teamList", teamName);
            }
            result = teamAddResult(id, teamName, settingDto);

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
            Long listSize = operations.opsForList().size("teamList");
            List<Object> teamList = operations.opsForList().range("teamList", 0, listSize-1);

            // 랭크 계급 설정
            rankList = rankListAdd(rankdto, teamList, settingDto);
    
            // List<String> rankFilterList = new ArrayList<>();
            List<String> positionList = new ArrayList<>();
    
            // // 랭크 필터링
            // for (Object key : teamList) {
            //     // 팀 사이즈 확인 
            //     if(hashOperations.size("match:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("match:"+key.toString()) > 0) {
            //         String name = String.valueOf(key).split("_")[0];
            //         for (int i = 0; i < rankList.size(); i++) {
            //             if(name.equals(rankList.get(i))) {
            //                 rankFilterList.add(key.toString());
            //             }
            //         }
            //     }
            // }    
    
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
                teamName = positionCheck(positionList, userId, min, max, true);
            }
            else {
                // 일치하는 mmr이 없을 경우
                log.info("새롭게 팀을 추가함 ");
                String uuid = UUID.randomUUID().toString();
                teamName = rankdto.getRankName()+"_"+min+"_"+max+"_"+uuid;
                teamCreate(teamName, userAllDto);
                hashOperations.put("position:"+teamName, position, "1");
                redisTemplate.opsForList().rightPush("teamList", teamName);
    
            }
            result = teamAddResult(id, teamName, settingDto);

        }
        return result;
    }

    private List<String> rankListAdd(RankDto rankdto, List<Object> teamList, SettingDto settingDto) {
        
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        List<RankDto> rankDtoList = mainMapper.findByRank();
        
        List<String> rankList = new ArrayList<>();

        List<String> rankFilterList = new ArrayList<>();

        if(rankdto.getRankLevel()==1) {
            rankList.add(rankDtoList.get(1).getRankName());
        }
        else if(rankdto.getRankLevel()==rankDtoList.size()) {
            rankList.add(rankDtoList.get(rankDtoList.size()-2).getRankName());
        }
        else {
            rankList.add(rankDtoList.get(rankdto.getRankLevel()-2).getRankName());
            rankList.add(rankDtoList.get(rankdto.getRankLevel()).getRankName());
        }
        rankList.add(rankdto.getRankName());

        // 랭크 필터링
        for (Object key : teamList) {
            // 팀 사이즈 확인 
            if(hashOperations.size("match:"+key.toString()) < (settingDto.getSettingHeadcount()*2) && hashOperations.size("match:"+key.toString()) > 0) {
                String name = String.valueOf(key).split("_")[0];
                for (int i = 0; i < rankList.size(); i++) {
                    if(name.equals(rankList.get(i))) {
                        rankFilterList.add(key.toString());
                    }
                }
            }
        }

        return rankFilterList;
    }

    // 동의 시간 저장
    private void acceptTime(String listName, int time) throws ParseException {
        
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        Calendar cal = Calendar.getInstance();
        // log.info("date : "+date);
        cal.setTime(date);
        cal.add(Calendar.SECOND, time);
        // log.info("date+time : "+cal.getTime());
        String saveTime = simpleDateFormat.format(cal.getTime());
        log.info("saveTime : "+saveTime);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        hashOperations.put("acceptTime", listName, saveTime);
    }

    // 팀에서 유저 정보 삭제 : 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : ALL
    public HashMap<String, String> teamListDeleteUser(int userId) throws Exception {

        HashMap<String, String> result = new HashMap<>();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        SettingDto settingDto = mainMapper.findBySettingId();

        String id = Integer.toString(userId);
        if(hashOperations.hasKey("matchAll", id)) {
            Object key = hashOperations.get("matchAll", id);

            UserAllDto user = objectMapper.readValue(hashOperations.get("match:"+key.toString(), id).toString(), UserAllDto.class);

            if(settingDto.getSettingType().equals("position") || settingDto.getSettingType().equals("all")) {

                String position = mainMapper.findByPositionId(user.getPositionId()).getPositionName();

                // 포지션 수 줄임
                Object userPosition = hashOperations.get("position:"+key.toString(), position);
        
                if(Integer.parseInt(userPosition.toString())==1) {
                    hashOperations.delete("position:"+key.toString(), position);
                }
                else if(Integer.parseInt(userPosition.toString())==2) {
                    hashOperations.put("position:"+key.toString(), userPosition, "1");
                }
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
    public HashMap<String, String> matchAcceptUser(int userId) throws Exception {
        
        HashMap<String, String> result = new HashMap<>();
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        boolean condition = true;

        // DB로 세팅 정보 가져오기
        SettingDto settingDto = mainMapper.findBySettingId();

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
                    continue;
                }
                // 설정한 시간이 안지났지만 정원이 다 차면 미리 바로 탈출
                if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                    result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        if(settingDto.getSettingType().equals("mmr") || settingDto.getSettingType().equals("rank")) {
                            mmrDivide(teamName, settingDto.getSettingHeadcount());
                        }
                        else {
                            positionDivide(teamName);
                        }

                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfo(teamName);                        
                    }
                    return result;
                }
                Thread.sleep(1000);
            }
            // log.info("size = "+hashOperations.size("accept:"+teamName));
            if(hashOperations.size("accept:"+teamName) == (settingDto.getSettingHeadcount()*2)) {
                result.put("code", "success");
                    result.put("listname", teamName);
                    if(condition) {
                        if(settingDto.getSettingType().equals("mmr") || settingDto.getSettingType().equals("rank")) {
                            mmrDivide(teamName, settingDto.getSettingHeadcount());
                        }
                        else {
                            positionDivide(teamName);
                        }
                        // 팀 정보 제외 전체 삭제
                        deleteMatchInfo(teamName);                        
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
                        
                        if(settingDto.getSettingType().equals("position") || settingDto.getSettingType().equals("all")) {
                            positionDelete(teamName, key);
                        }
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

    // match, matchAll, acceptTime을 지운다
    private void commonDelete(String teamName, Object key) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        hashOperations.delete("match:"+teamName, key);
        hashOperations.delete("matchAll", key);

    }

    // 포지션 지우기
    private void positionDelete(String teamName, Object key) throws JsonMappingException, JsonProcessingException {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        UserAllDto userAllDto = objectMapper.readValue(hashOperations.get("match:"+teamName, key).toString(), UserAllDto.class);
        String position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();
        
        Object object = hashOperations.get("position:"+teamName, position);
        
        if(object.toString().equals("2")) {
            hashOperations.put("position:"+teamName, position, "1");
        }
        else {
            hashOperations.delete("position:"+teamName, position);
        }
    }



    // 팀 배정 정보 및 본인이 속한 팀 정보 주기, 에외 처리 고민
    public GroupMatchDto matchCompleteUser(int userId, String teamName) throws Exception {
        
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

        GroupMatchDto groupMatchDto = new GroupMatchDto(userInfo, teamInfo(teamAMap), teamInfo(teamBMap));
        
        return groupMatchDto;

    }

    // matchAll 삭제
    public void deleteMatchAll(String teamName) {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        int size = hashOperations.entries("match:"+teamName).size();

        Map<Object, Object> allMap = hashOperations.entries("matchAll");
        int count = 0;
        
        for(Object key : allMap.keySet()) {
            if(hashOperations.get("matchAll", key).toString().equals(teamName)) {
                hashOperations.delete("matchAll", key);
                count += 1;
            }
            // 팀에 들어있는 사이즈만큼 다 지우면 바로 탈출
            if(count==size) {
                break;
            }
        }
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

    // 매칭 진행 : 팀 사이즈 확인, 정원이 다 차면 메소드 탈출
    private String teamCheck(Long size, String listName, String id, int headCount, int count) throws InterruptedException, JsonMappingException, JsonProcessingException {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Boolean condition = true;
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR, 1);

        while(condition) {
            Date now = new Date();
            if(now.after(cal.getTime())) {
                return "auto_cancel";
            }
            if(hashOperations.hasKey("match:"+listName, id)==false) {
                return "cancel";
            }
            if(hashOperations.size("match:"+listName) < headCount) {
                continue;
            }
            if(hashOperations.size("match:"+listName) == headCount) {
                condition = false;
            }
            Thread.sleep(1000);
        }
        return "ok";
        
    }

    // private List<String> rankListAdd(RankDto rankDto) {
        
    //     List<RankDto> rankDtoList = mainMapper.findByRank();
        
    //     List<String> rankList = new ArrayList<>();

    //     if(rankDto.getRankLevel()==1) {
    //         rankList.add(rankDtoList.get(1).getRankName());
    //     }
    //     else if(rankDto.getRankLevel()==rankDtoList.size()) {
    //         rankList.add(rankDtoList.get(rankDtoList.size()-2).getRankName());
    //     }
    //     else {
    //         rankList.add(rankDtoList.get(rankDto.getRankLevel()-2).getRankName());
    //         rankList.add(rankDtoList.get(rankDto.getRankLevel()).getRankName());
    //     }
    //     rankList.add(rankDto.getRankName());

    //     return rankList;
    // }

    // all team 생성
    private void teamCreate(String teamName, UserAllDto userAllDto) throws JsonProcessingException {
        String id = Integer.toString(userAllDto.getUserId());
        
        userAllDto.setTeamName(teamName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userAllDto);
        hashOperations.put("match:"+teamName, id, user);
    }

    // mmr team 생성
    private void teamCreate(String teamName, UserMmrDto userMmrDto) throws JsonProcessingException {
        String id = Integer.toString(userMmrDto.getUserId());
        
        userMmrDto.setTeamName(teamName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userMmrDto);
        hashOperations.put("match:"+teamName, id, user);
    }

    // position team 생성
    private void teamCreate(String teamName, UserPositionDto userPositionDto) throws JsonProcessingException {
        String id = Integer.toString(userPositionDto.getUserId());
        
        userPositionDto.setTeamName(teamName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userPositionDto);
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

    // 포지션 확인
    private String positionCheck(List<String> positionList, int userId, int min, int max, Boolean rankContain) throws JsonProcessingException {

        String rank = "";
        String position = "";
        UserAllDto userAllDto = new UserAllDto();
        UserPositionDto userPositionDto = new UserPositionDto();
        if(rankContain) {
        userAllDto = mainMapper.findByAllUserId(userId);
        rank = mainMapper.findByRankId(userAllDto.getRankId()).getRankName();
        position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();
        }
        else {
            userPositionDto = mainMapper.findByPositionUserId(userId);
            position = mainMapper.findByPositionId(userPositionDto.getPositionId()).getPositionName();
        }
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String teamName = "";

        for (int i = 0; i < positionList.size(); i++) {
            // 포지션 조건 추가
            if(hashOperations.hasKey("position:"+positionList.get(i), position)) {
                if(Integer.valueOf(hashOperations.get("position:"+positionList.get(i), position).toString())>1) {
                    if(i==positionList.size()-1) {
                        String uuid = UUID.randomUUID().toString();
                        if(rank.equals("")) {
                            teamName = min+"_"+max+"_"+uuid;
                            teamCreate(teamName, userPositionDto);
                        }
                        else {
                        teamName = rank+"_"+min+"_"+max+"_"+uuid;
                        teamCreate(teamName, userAllDto);
                        }
                        log.info("일치하는 mmr이 있으나 포지션이 없어서 팀 새로 생성");
                        hashOperations.put("position:"+teamName, position, "1");
                        redisTemplate.opsForList().rightPush("teamList", teamName);
                        return teamName;
                    }
                    else {
                        // 해당 팀에 포지션이 없으나, 다음 후보가 있을 경우
                        continue;
                    }
                }
                else {
                    // 포지션 자리가 존재해 기존의 팀에 값 추가, 포지션 자리가 1인경우, 0인경우
                    teamName = positionList.get(i);
                    if(rank.equals("")) {
                        teamCreate(teamName, userPositionDto);
                    }
                    else {
                    teamCreate(teamName, userAllDto);
                    }
                    hashOperations.put("position:"+teamName, position, "2");
                    return teamName;
                }
            }
            else {
                // 포지션 자리가 존재해 기존의 팀에 값 추가, 포지션 자리가 0인경우, 없는 경우
                teamName = positionList.get(i);
                if(rank.equals("")) {
                    teamCreate(teamName, userPositionDto);
                }
                else {
                teamCreate(teamName, userAllDto);
                }
                hashOperations.put("position:"+teamName, position, "1");
                return teamName;
            }
        }

        return teamName;

    }

    // 팀 정보 지우기 : map, accept, acceptTime, position
    public void deleteMatchInfo(String listName) {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        SettingDto settingDto = mainMapper.findBySettingId();

        hashOperations.getOperations().delete("match:"+listName);        
        hashOperations.getOperations().delete("accept:"+listName);
        hashOperations.delete("acceptTime", listName);

        if(settingDto.getSettingType().equals("position") || settingDto.getSettingType().equals("all")) {
            hashOperations.getOperations().delete("position:"+listName);
        }
    }

    // mmr 팀 나누기 & 랭크 팀 나누기
    public void mmrDivide(String teamName, int headCount) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        Map<Object, Object> map = hashOperations.entries("accept:"+teamName);

        List<UserAllDto> userListA = new ArrayList<>();

        List<UserAllDto> userListB = new ArrayList<>();

        // mmr 나누기
        for(Object key : map.keySet() ){
            UserAllDto user = objectMapper.readValue(map.get(key).toString(), UserAllDto.class);
            userListA.add(user);
        }

        HashMap<Integer, List<List<UserAllDto>>> teamResult = new HashMap<>();

        mmrCombi(teamResult, userListA, userListB, headCount);

        for(Integer key : teamResult.keySet()) {
            for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
                UserAllDto userAllDtoA = teamResult.get(key).get(0).get(i);
                UserAllDto userAllDtoB = teamResult.get(key).get(1).get(i);
                hashOperations.put("teamA:"+teamName, Integer.toString(userAllDtoA.getUserId()), objectMapper.writeValueAsString(userAllDtoA));
                hashOperations.put("teamB:"+teamName, Integer.toString(userAllDtoB.getUserId()), objectMapper.writeValueAsString(userAllDtoB));
            }
            log.info("최종값 : " + key);
        }
    }

    private void mmrCombi(HashMap<Integer, List<List<UserAllDto>>> teamResult, List<UserAllDto> userListA, List<UserAllDto> userListB, int count) {
        
        if(count == 0) {
            int sumA = 0;
            int sumB = 0;
            List<UserAllDto> userA = new ArrayList<>();
            List<UserAllDto> userB = new ArrayList<>();

            for (int i = 0; i < userListA.size(); i++) {
                sumA += userListA.get(i).getUserMmr();
                sumB += userListB.get(i).getUserMmr();
                userB.add(userListB.get(i));
                userA.add(userListA.get(i));
            }

            int sumDif = Math.abs(sumA - sumB);
            List<List<UserAllDto>> teamList = new ArrayList<>();
           
            teamList.add(userA);
            teamList.add(userB);

            // log.info("합A : " + sumA + " 합B : " + sumB + " 차이 : "+sumDif);

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
    public void positionDivide(String teamName) throws JsonMappingException, JsonProcessingException {

        // 키 값 돌려서 포지션 알아내서 비교하기
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        List<PositionDto> positionAllList = mainMapper.findByPosition();

        Map<Object, Object> map = hashOperations.entries("accept:"+teamName);

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

        for(Integer key : teamResult.keySet())
        for (int i = 0; i < teamResult.get(key).get(0).size(); i++) {
            UserAllDto userAllDtoA = teamResult.get(key).get(0).get(i);
            UserAllDto userAllDtoB = teamResult.get(key).get(1).get(i);
            hashOperations.put("teamA:"+teamName, Integer.toString(userAllDtoA.getUserId()), objectMapper.writeValueAsString(userAllDtoA));
            hashOperations.put("teamB:"+teamName, Integer.toString(userAllDtoB.getUserId()), objectMapper.writeValueAsString(userAllDtoB));
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
