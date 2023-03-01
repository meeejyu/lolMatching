package com.lol.match;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lol.match.common.exception.BusinessLogicException;
import com.lol.match.common.exception.ExceptionCode;
import com.lol.match.main.model.UserAllDto;
import com.lol.match.main.model.UserMmrDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class test {

    @Test
	void timeTest() {

        LocalDateTime time = LocalDateTime.now();

		String nowTime = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		System.out.println("시간 : " + nowTime);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date nowDate = sdf.parse(nowTime);
            Date originalDate = sdf.parse("match:Gold_526_626_2022-12-20 15:18:11_6d0956d7-59e7-4e99-a94e-90d6ef13b30b".split("_")[3]);

            if(nowDate.after(originalDate)) { // 미래의 시간.after(과거시간) => true
                System.out.println("리스트 및 hashMap 변경");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
	}

    @Test
    void redisTest(){

        int n = 10;
        int r = 5;
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        boolean[] visited = new boolean[n];

        System.out.println("\n" + n + " 개 중에서 " + r + " 개 뽑기");
        combination(arr, visited, 0, n, r);

    }

    void combination(int[] arr, boolean[] visited, int start, int n, int r) {
        if (r == 0) {
            print(arr, visited, n);
            return;
        }

        for (int i = start; i < n; i++) {
            visited[i] = true;
            combination(arr, visited, i + 1, n, r - 1);
            visited[i] = false;
        }
    }

    // 배열 출력
    void print(int[] arr, boolean[] visited, int n) {
        for (int i = 0; i < n; i++) {
            if (visited[i]) {
                System.out.print(arr[i] + " ");
            }
        }
        System.out.println();
    }

    @Test
    void mmrTest(){
        
        List<UserMmrDto> userInfoA = new ArrayList<>();

        List<UserMmrDto> userListAll = new ArrayList<>();

        // userListAll.add(new UserMmrDto(1, 501));
        // userListAll.add(new UserMmrDto(2, 525));

        userInfoA.add(userListAll.get(0));
        HashMap<Integer, List<List<UserMmrDto>>> teamResult = new HashMap<>();
        mmrCombi(teamResult, userListAll, userInfoA, 1, 1);


        System.out.println(teamResult);
    }

    void mmrCombi(HashMap<Integer, List<List<UserMmrDto>>> teamResult, List<UserMmrDto> userListAll, List<UserMmrDto> userInfoA, int headCount, int count) {    

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

            log.info("A : " + userInfoA.get(0).getUserId()); 
            log.info("B : " + userInfoB.get(0).getUserId());

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
                    if(sumDif==0) {
                        return;
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

    @Test
    void allTest(){
        
        List<List<UserAllDto>> userPositionList = new ArrayList<>(); 

        List<UserAllDto> userInfoA = new ArrayList<>();

        HashMap<Integer, List<List<UserAllDto>>> teamResult = new HashMap<>();

        userPositionList.add(new ArrayList<>());

        // userPositionList.get(0).add(new UserAllDto(1, 501));
        // userPositionList.get(0).add(new UserAllDto(2, 525));

        userInfoA.add(userPositionList.get(0).get(0));

        // A팀 - B팀의 값이 최솟값일 경우 팀 매칭
        allCombi(userInfoA, 1, userPositionList, teamResult, 1);

        System.out.println(teamResult);
    }

    private void allCombi(List<UserAllDto> userInfoA, int count, 
    List<List<UserAllDto>> userPositionList, HashMap<Integer, List<List<UserAllDto>>> teamResult, int headCount) {
        
        if(count == headCount) {
            List<UserAllDto> userInfoB = new ArrayList<>();

            for (int i = 0; i < userInfoA.size(); i++) {
                for (int j = 0; j < userPositionList.get(i).size(); j++) {
                    if(userInfoA.get(i)!=userPositionList.get(i).get(j)) {
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
            log.info("A : " + userInfoA.get(0).getUserId()); 
            log.info("B : " + userInfoB.get(0).getUserId());
            
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
                return;
            }
            else {
                for(Integer key : teamResult.keySet()){
                    if(key > sumDif) {
                        teamResult.clear();
                        teamResult.put(sumDif, teamList);
                    }
                    if(sumDif==0) {
                        return;
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
}
