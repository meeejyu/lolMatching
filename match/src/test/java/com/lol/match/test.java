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

        // int n = 6;
        // int[] arr = {1, 2, 3, 4, 5, 6};

        int n = 8;
        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8};
        boolean[] visited = new boolean[n];

        // System.out.println("\n" + n + " 개 중에서 " + 3 + " 개 뽑기");
        // combination(arr, visited, 0, n, 3);

        System.out.println("\n" + n + " 개 중에서 " + 4 + " 개 뽑기");
        combination(arr, visited, 0, n, 4);

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
        cc = cc+1;
        log.info("카운트 : "+(cc));
    }

    int cc = 0;
    @Test
    void tsdf(){
        
        List<UserMmrDto> userListA = new ArrayList<>();

        List<UserMmrDto> userListAll = new ArrayList<>();
        List<UserMmrDto> userListB = new ArrayList<>();

        userListAll.add(new UserMmrDto(1, 501));
        userListAll.add(new UserMmrDto(2, 525));
        userListAll.add(new UserMmrDto(3, 540));
        userListAll.add(new UserMmrDto(4, 551));
        userListAll.add(new UserMmrDto(5, 456));
        userListAll.add(new UserMmrDto(6, 472));
        userListAll.add(new UserMmrDto(7, 493));
        userListAll.add(new UserMmrDto(8, 536));



        userListA.add(userListAll.get(0));
        HashMap<Integer, List<List<UserMmrDto>>> teamResult = new HashMap<>();
        mmrCombitt(teamResult, userListAll, userListA, userListB, 4, 1);

    }

    void mmrCombitt(HashMap<Integer, List<List<UserMmrDto>>> teamResult, List<UserMmrDto> userListAll,List<UserMmrDto> userListA, List<UserMmrDto> userListB, int headCount, int count) {
        
        if(userListA.size() == headCount) {
            for (int j = 0; j < userListAll.size(); j++) {
                if(userListA.contains(userListAll.get(j))==false) {
                    userListB.add(userListAll.get(j));
                }
            }
            int sumA = 0;
            int sumB = 0;
            List<List<UserMmrDto>> teamList = new ArrayList<>();
            List<UserMmrDto> userA = new ArrayList<>();
            List<UserMmrDto> userB = new ArrayList<>();

            for (int j = 0; j < userListA.size(); j++) {
                sumA += userListA.get(j).getUserMmr();
                sumB += userListB.get(j).getUserMmr();
                userA.add(userListA.get(j));
                userB.add(userListB.get(j));
            }
            int sumDif = Math.abs(sumA - sumB);

            log.info("A : " + userA.get(0).getUserId()+","+ userA.get(1).getUserId()+","+ userA.get(2).getUserId()+","+ userA.get(3).getUserId()); 
            log.info("B : " + userB.get(0).getUserId()+","+ userB.get(1).getUserId()+","+ userB.get(2).getUserId()+","+ userB.get(3).getUserId());
            log.info("합A : " + sumA + " 합B : " + sumB + " 차이 : "+sumDif);
            cc = cc+1;
            log.info("카운트 : "+(cc));
            teamList.add(userA);
            teamList.add(userB);
            
            for(Integer key : teamResult.keySet() ){
                if(key > sumDif) {
                    teamResult.remove(key);
                    teamResult.put(sumDif, teamList);
                }
                if(sumDif==0) {
                    return;
                }
            }
            if(teamResult.size() == 0) {
                teamResult.put(sumDif, teamList);
            }
            userListB.clear();
        }
        else {
            for (int i = count; i < userListAll.size(); i++) {
                userListA.add(userListAll.get(i));
                mmrCombitt(teamResult, userListAll, userListA, userListB, headCount, i+1);
                userListA.remove(userListAll.get(i));                    
            }
        }
    }
}
