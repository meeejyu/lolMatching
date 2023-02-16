package com.lol.match.main.controller;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lol.match.main.model.GroupMatchAllDto;
import com.lol.match.main.model.GroupMatchMmrDto;
import com.lol.match.main.model.GroupMatchPositionDto;
import com.lol.match.main.model.GroupMatchRankDto;
import com.lol.match.main.service.MainService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    int count = 0;

    @GetMapping("/test")
    public String testHtml() {

        return "test";
    }


    // MMR 매칭 start -----------
    @PostMapping("/match/mmr")
    @ResponseBody
    public HashMap<String, String> matchMmr(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.matchUserMmr(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/mmr/cancel")
    @ResponseBody
    public HashMap<String, String> teamListDeleteMmr(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.teamListDeleteUserMmrRank(userId);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/mmr/accept")
    @ResponseBody
    public HashMap<String, String> matchAcceptMmr(@RequestParam int userId) throws Exception {
        
        HashMap<String, String> result = mainService.matchAcceptUserMmr(userId);

        return result;
    }

    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/mmr/complete")
    @ResponseBody
    public GroupMatchMmrDto matchCompleteMmr(@RequestParam int userId, String teamName) throws Exception {
        
        GroupMatchMmrDto groupMatchDto = mainService.matchCompleteUserMmr(userId, teamName);
        
        return groupMatchDto;

    }

    // 매칭이 완료됐지만 아무도 수락하지 않은 경우 해당 메소드 실행
    @DeleteMapping("/match/mmr/delete/{listName}")
    @ResponseBody
    public String deleteMmr(@PathVariable String listName) {
        
        mainService.deleteMatchInfoMmrRank(listName);
        mainService.deleteMatchAll(listName);

        return "ok";
    }
    // MMR 매칭 end -----------


    // All(포지션, 랭크, MMR) 매칭 start -----------
    @PostMapping("/match/all")
    @ResponseBody
    public HashMap<String, String> matchAll(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.matchUserAll(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/all/cancel")
    @ResponseBody
    public HashMap<String, String> teamListDeleteAll(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.teamListDeleteUserAllPosition(userId);

        return result;
    }


    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/all/accept")
    @ResponseBody
    public HashMap<String, String> matchAcceptAll(@RequestParam int userId) throws Exception {
        
        HashMap<String, String> result = mainService.matchAcceptUserAll(userId);

        return result;
    }

    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/all/complete")
    @ResponseBody
    public GroupMatchAllDto matchCompleteAll(@RequestParam int userId, String teamName) throws Exception {
        
        GroupMatchAllDto groupMatchDto = mainService.matchCompleteUserAll(userId, teamName);
        
        return groupMatchDto;

    }

    // 매칭이 완료됐지만 아무도 수락하지 않은 경우 해당 메소드 실행
    @DeleteMapping("/match/All/delete/{listName}")
    @ResponseBody
    public String deleteAll(@PathVariable String listName) {
        
        mainService.deleteMatchInfoAllPosition(listName);
        mainService.deleteMatchAll(listName);

        return "ok";
    }
    // All(포지션, 랭크, MMR) 매칭 end -----------


    // Position(Position, MMR) 매칭 start -----------
    @PostMapping("/match/position")
    @ResponseBody
    public HashMap<String, String> matchPosition(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.matchUserPosition(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/position/cancel")
    @ResponseBody
    public HashMap<String, String> teamListDeletePosition(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.teamListDeleteUserAllPosition(userId);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    // TODO : 여기부터 다시
    @PostMapping("/match/position/accept")
    @ResponseBody
    public HashMap<String, String> matchAcceptPosition(@RequestParam int userId) throws Exception {
        
        HashMap<String, String> result = mainService.matchAcceptUserPosition(userId);

        return result;
    }
    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/position/complete")
    @ResponseBody
    public GroupMatchPositionDto matchCompletePosition(@RequestParam int userId, String teamName) throws Exception {
        
        GroupMatchPositionDto groupMatchDto = mainService.matchCompleteUserPosition(userId, teamName);
        
        return groupMatchDto;

    }
    // Position(Position, MMR) 매칭 end -----------


    // Rank(Rank, MMR) 매칭 start -----------
    @PostMapping("/match/rank")
    @ResponseBody
    public HashMap<String, String> matchRank(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.matchUserRank(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/rank/cancel")
    @ResponseBody
    public HashMap<String, String> teamListDeleteRank(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.teamListDeleteUserMmrRank(userId);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/rank/accept")
    @ResponseBody
    public HashMap<String, String> matchAcceptRank(@RequestParam int userId) throws Exception {
        
        HashMap<String, String> result = mainService.matchAcceptUserRank(userId);

        return result;
    }

    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/rank/complete")
    @ResponseBody
    public GroupMatchRankDto matchCompleteRank(@RequestParam int userId, String teamName) throws Exception {
        
        GroupMatchRankDto groupMatchDto = mainService.matchCompleteUserRank(userId, teamName);
        
        return groupMatchDto;

    }
    // Rank(Rank, MMR) 매칭 end -----------


    // 게임이 종료된 이후 해당 메소드 실행
    @DeleteMapping("/match/end/{listName}")
    @ResponseBody
    public String matchEnd(@PathVariable String listName) {
        
        mainService.deleteMatchAll(listName);

        return "ok";
    }
    
    // 매칭이 완료됐지만 아무도 수락하지 않은 경우 해당 메소드 실행 : mmr, rank
    @DeleteMapping("/match/delete/mmr/rank/{listName}")
    @ResponseBody
    public String deleteMmrRank(@PathVariable String listName) {
        
        mainService.deleteMatchInfoMmrRank(listName);
        mainService.deleteMatchAll(listName);

        return "ok";
    }

    // 매칭이 완료됐지만 아무도 수락하지 않은 경우 해당 메소드 실행 : all, position
    @DeleteMapping("/match/delete/all/position/{listName}")
    @ResponseBody
    public String deleteAllPosition(@PathVariable String listName) {
        
        mainService.deleteMatchInfoAllPosition(listName);
        mainService.deleteMatchAll(listName);

        return "ok";
    }

    // 매칭 테스트 코드
    @PostMapping("/match/test")
    @ResponseBody
    public HashMap<String, String> matchTest() throws Exception {

        // 테스트 코드
        HashMap<String, String> result = new HashMap<>();
        count += 1;
        System.out.println("count : " + count);
        if(count < 7) {
            // result = mainService.matchUserMmr(count);
            // result = mainService.matchUserAll(count);
            result = mainService.matchUserPosition(count);
            // result = mainService.matchUserRank(count);
        }
        if(count == 7) {
            count = 0;
        }
        
        return result;
    }

    // 매칭 수락 테스트 코드
    @PostMapping("/match/accept/test")
    @ResponseBody
    public HashMap<String, String> matchAcceptTest() throws Exception {
        
        // 테스트 코드
        HashMap<String, String> result = new HashMap<>();
        count += 1;
        System.out.println("count : " + count);
        if(count < 7) {
            // result = mainService.matchAcceptUserMmr(count);
            // result = mainService.matchAcceptUserAll(count);
            result = mainService.matchAcceptUserPosition(count);
            // result = mainService.matchAcceptUserRank(count);
        }
        if(count == 7) {
            count = 0;
        }
        System.out.println("최종결과 : "+result.toString());
        
        return result;
    }


}
