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
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lol.match.main.mapper.MainMapper;
import com.lol.match.main.model.RankingDto;
import com.lol.match.main.model.SettingDto;
import com.lol.match.main.model.UserAllDto;
import com.lol.match.main.model.UserPositionDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CommonService {

    protected final RedisTemplate<String, Object> redisTemplate;

    protected final MainMapper mainMapper;

    protected final ObjectMapper objectMapper;

    protected HashMap<String, String> teamAddResult(String id, String teamName, SettingDto settingDto) throws JsonMappingException, JsonProcessingException, InterruptedException, ParseException {
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

    protected List<String> rankListAdd(RankingDto rankdto, List<Object> teamList, SettingDto settingDto) {
        
        HashOperations<String, String, Object> hashOperations = redisTemplate.opsForHash();

        List<RankingDto> rankDtoList = mainMapper.findByRanking();
        
        List<String> rankList = new ArrayList<>();

        List<String> rankFilterList = new ArrayList<>();

        if(rankdto.getRankingLevel()==1) {
            rankList.add(rankDtoList.get(1).getRankingName());
        }
        else if(rankdto.getRankingLevel()==rankDtoList.size()) {
            rankList.add(rankDtoList.get(rankDtoList.size()-2).getRankingName());
        }
        else {
            rankList.add(rankDtoList.get(rankdto.getRankingLevel()-2).getRankingName());
            rankList.add(rankDtoList.get(rankdto.getRankingLevel()).getRankingName());
        }
        rankList.add(rankdto.getRankingName());

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

    // match, matchAll을 지운다
    protected void commonDelete(String teamName, Object key) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        hashOperations.delete("match:"+teamName, key);
        hashOperations.delete("matchAll", key);

    }

    // 포지션 지우기
    protected void positionDelete(String teamName, Object key) throws JsonMappingException, JsonProcessingException {

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

    // matchAll
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

            System.out.println("id : " + id + " 시간 : "+ now);
            
            if(now.after(cal.getTime())) {
                return "auto_cancel";
            }
            if(hashOperations.hasKey("match:"+listName, id)==false) {
                return "cancel";
            }
            if(hashOperations.size("match:"+listName) < headCount) {
                Thread.sleep(1000);
                continue;
            }
            if(hashOperations.size("match:"+listName) == headCount) {
                condition = false;
            }
        }
        return "ok";
        
    }

    // all team 생성
    protected void teamCreate(String teamName, UserAllDto userAllDto) throws JsonProcessingException {
        String id = Integer.toString(userAllDto.getUserId());
        
        userAllDto.setTeamName(teamName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userAllDto);
        hashOperations.put("match:"+teamName, id, user);
    }

    

    // position team 생성
    protected void teamCreate(String teamName, UserPositionDto userPositionDto) throws JsonProcessingException {
        String id = Integer.toString(userPositionDto.getUserId());
        
        userPositionDto.setTeamName(teamName);
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        String user = objectMapper.writeValueAsString(userPositionDto);
        hashOperations.put("match:"+teamName, id, user);
    }



    // 포지션 확인
    protected String positionCheck(List<String> positionList, int userId, int min, int max) throws JsonProcessingException {

        ListOperations<String, Object> listOperations = redisTemplate.opsForList();

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        UserPositionDto userPositionDto = mainMapper.findByPositionUserId(userId);
        String position = mainMapper.findByPositionId(userPositionDto.getPositionId()).getPositionName();

        String teamName = "";

        for (int i = 0; i < positionList.size(); i++) {
            // 포지션 조건 추가
            if(hashOperations.hasKey("position:"+positionList.get(i), position)) {
                if(Integer.valueOf(hashOperations.get("position:"+positionList.get(i), position).toString())>1) {
                    if(i==positionList.size()-1) {
                        String uuid = UUID.randomUUID().toString();
                        teamName = min+"_"+max+"_"+uuid;
                        teamCreate(teamName, userPositionDto);
                        
                        log.info("일치하는 mmr이 있으나 포지션이 없어서 팀 새로 생성");
                        hashOperations.put("position:"+teamName, position, "1");
                        listOperations.rightPush("teamList", teamName);
                        return teamName;
                    }
                }
                else {
                    // 포지션 자리가 존재해 기존의 팀에 값 추가, 포지션 자리가 1인경우, 0인경우
                    teamName = positionList.get(i);
                    teamCreate(teamName, userPositionDto);
                    hashOperations.put("position:"+teamName, position, "2");
                    return teamName;
                }
            }
            else {
                // 포지션 자리가 존재해 기존의 팀에 값 추가, 포지션 자리가 0인경우, 없는 경우
                teamName = positionList.get(i);
                teamCreate(teamName, userPositionDto);
                hashOperations.put("position:"+teamName, position, "1");
                return teamName;
            }
        }

        return teamName;

    }

    protected String allCheck(List<String> positionList, int userId, int min, int max) throws JsonProcessingException {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        ListOperations<String, Object> listOperations = redisTemplate.opsForList();

        UserAllDto userAllDto = mainMapper.findByAllUserId(userId);
        String ranking = mainMapper.findByRankingId(userAllDto.getRankingId()).getRankingName();
        String position = mainMapper.findByPositionId(userAllDto.getPositionId()).getPositionName();

        String teamName = "";

        for (int i = 0; i < positionList.size(); i++) {
            // 포지션 조건 추가
            if(hashOperations.hasKey("position:"+positionList.get(i), position)) {
                if(Integer.valueOf(hashOperations.get("position:"+positionList.get(i), position).toString())>1) {
                    if(i==positionList.size()-1) {
                        String uuid = UUID.randomUUID().toString();
                        teamName = ranking+"_"+min+"_"+max+"_"+uuid;
                        teamCreate(teamName, userAllDto);

                        log.info("일치하는 mmr이 있으나 포지션이 없어서 팀 새로 생성");
                        hashOperations.put("position:"+teamName, position, "1");
                        listOperations.rightPush("teamList", teamName);
                        return teamName;
                    }
                }
                else {
                    // 포지션 자리가 존재해 기존의 팀에 값 추가, 포지션 자리가 1인경우
                    teamName = positionList.get(i);
                    teamCreate(teamName, userAllDto);
                    hashOperations.put("position:"+teamName, position, "2");
                    return teamName;
                }
            }
            else {
                // 포지션 자리가 존재해 기존의 팀에 값 추가, 포지션 자리가 0인경우, 없는 경우
                teamName = positionList.get(i);
                teamCreate(teamName, userAllDto);
                hashOperations.put("position:"+teamName, position, "1");
                return teamName;
            }
        }

        return teamName;

    }

    // 팀 정보 지우기 : map, accept, acceptTime
    public void deleteMatchInfoMmrRanking(String listName) {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        hashOperations.getOperations().delete("match:"+listName);        
        hashOperations.getOperations().delete("accept:"+listName);
        hashOperations.delete("acceptTime", listName);
    }

    // 팀 정보 지우기 : map, accept, acceptTime, position
    public void deleteMatchInfoAllPosition(String listName) {

        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        deleteMatchInfoMmrRanking(listName);

        hashOperations.getOperations().delete("position:"+listName);
    }
   

}
