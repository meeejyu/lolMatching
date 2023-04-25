package com.lol.match.main.controller;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lol.match.main.model.GroupMatchAllDto;
import com.lol.match.main.model.GroupMatchMmrDto;
import com.lol.match.main.model.GroupMatchPositionDto;
import com.lol.match.main.model.GroupMatchRankDto;
import com.lol.match.main.service.AllPositionService;
import com.lol.match.main.service.MmrRankService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {

    // private final CommonService commonService;

    private final MmrRankService mmrRankService;

    private final AllPositionService allPositionService;

    int count = 0;

    @GetMapping("/test")
    public String testHtml() {

        return "test";
    }


    // MMR 매칭 start -----------
    @PostMapping("/match/mmr/{userId}")
    @ResponseBody
    public HashMap<String, String> matchMmr(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = mmrRankService.matchUserMmr(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/mmr/cancel/{userId}")
    @ResponseBody
    public HashMap<String, String> teamListDeleteMmr(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = mmrRankService.teamListDeleteUserMmrRank(userId);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/mmr/accept/{userId}")
    @ResponseBody
    public HashMap<String, String> matchAcceptMmr(@PathVariable int userId) throws Exception {
        
        HashMap<String, String> result = mmrRankService.matchAcceptUserMmr(userId);

        return result;
    }

    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/mmr/complete/{userId}/{teamName}")
    @ResponseBody
    public GroupMatchMmrDto matchCompleteMmr(@PathVariable int userId, @PathVariable String teamName) throws Exception {
        
        GroupMatchMmrDto groupMatchDto = mmrRankService.matchCompleteUserMmr(userId, teamName);
        
        return groupMatchDto;

    }

    // MMR 매칭 end -----------


    // All(포지션, 랭크, MMR) 매칭 start -----------
    @PostMapping("/match/all/{userId}")
    @ResponseBody
    public HashMap<String, String> matchAll(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = allPositionService.matchUserAll(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/all/cancel/{userId}")
    @ResponseBody
    public HashMap<String, String> teamListDeleteAll(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = allPositionService.teamListDeleteUserAllPosition(userId);

        return result;
    }


    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/all/accept/{userId}")
    @ResponseBody
    public HashMap<String, String> matchAcceptAll(@PathVariable int userId) throws Exception {
        
        HashMap<String, String> result = allPositionService.matchAcceptUserAll(userId);

        return result;
    }

    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/all/complete/{userId}/{teamName}")
    @ResponseBody
    public GroupMatchAllDto matchCompleteAll(@PathVariable int userId, @PathVariable String teamName) throws Exception {
        
        GroupMatchAllDto groupMatchDto = allPositionService.matchCompleteUserAll(userId, teamName);
        
        return groupMatchDto;

    }

    // All(포지션, 랭크, MMR) 매칭 end -----------


    // Position(Position, MMR) 매칭 start -----------
    @PostMapping("/match/position/{userId}")
    @ResponseBody
    public HashMap<String, String> matchPosition(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = allPositionService.matchUserPosition(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/position/cancel/{userId}")
    @ResponseBody
    public HashMap<String, String> teamListDeletePosition(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = allPositionService.teamListDeleteUserAllPosition(userId);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/position/accept/{userId}")
    @ResponseBody
    public HashMap<String, String> matchAcceptPosition(@PathVariable int userId) throws Exception {
        
        HashMap<String, String> result = allPositionService.matchAcceptUserPosition(userId);

        return result;
    }
    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/position/complete/{userId}/{teamName}")
    @ResponseBody
    public GroupMatchPositionDto matchCompletePosition(@PathVariable int userId, @PathVariable String teamName) throws Exception {
        
        GroupMatchPositionDto groupMatchDto = allPositionService.matchCompleteUserPosition(userId, teamName);
        
        return groupMatchDto;

    }
    // Position(Position, MMR) 매칭 end -----------


    // Rank(Rank, MMR) 매칭 start -----------
    @PostMapping("/match/rank/{userId}")
    @ResponseBody
    public HashMap<String, String> matchRank(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = mmrRankService.matchUserRank(userId);
        
        return result;
    }

    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/rank/cancel/{userId}")
    @ResponseBody
    public HashMap<String, String> teamListDeleteRank(@PathVariable int userId) throws Exception {

        HashMap<String, String> result = mmrRankService.teamListDeleteUserMmrRank(userId);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/rank/accept/{userId}")
    @ResponseBody
    public HashMap<String, String> matchAcceptRank(@PathVariable int userId) throws Exception {
        
        HashMap<String, String> result = mmrRankService.matchAcceptUserRank(userId);

        return result;
    }

    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/rank/complete/{userId}/{teamName}")
    @ResponseBody
    public GroupMatchRankDto matchCompleteRank(@PathVariable int userId, @PathVariable String teamName) throws Exception {
        
        GroupMatchRankDto groupMatchDto = mmrRankService.matchCompleteUserRank(userId, teamName);
        
        return groupMatchDto;

    }
    // Rank(Rank, MMR) 매칭 end -----------


    // 게임이 종료된 이후 해당 메소드 실행
    @DeleteMapping("/match/end/{listName}")
    @ResponseBody
    public String matchEnd(@PathVariable String listName) {
        
        mmrRankService.deleteMatchAll(listName);

        return "ok";
    }
    
    // 매칭이 완료됐지만 아무도 수락하지 않은 경우 해당 메소드 실행 : mmr, rank
    @DeleteMapping("/match/delete/mmr/rank/{listName}")
    @ResponseBody
    public String deleteMmrRank(@PathVariable String listName) {
        
        mmrRankService.deleteMatchInfoMmrRank(listName);
        mmrRankService.deleteMatchAll(listName);

        return "ok";
    }

    // 매칭이 완료됐지만 아무도 수락하지 않은 경우 해당 메소드 실행 : all, position
    @DeleteMapping("/match/delete/all/position/{listName}")
    @ResponseBody
    public String deleteAllPosition(@PathVariable String listName) {
        
        allPositionService.deleteMatchInfoAllPosition(listName);
        allPositionService.deleteMatchAll(listName);

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

        if(count < 13) {
            // result = mmrRankService.matchUserMmr(count);
            // result = allPositionService.matchUserAll(count);
            result = allPositionService.matchUserPosition(count);
            // result = mmrRankService.matchUserRank(count);
        }
        if(count == 13) {
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

        if(count < 13) {
            // result = mmrRankService.matchAcceptUserMmr(count);
            // result = allPositionService.matchAcceptUserAll(count);
            result = allPositionService.matchAcceptUserPosition(count);
            // result = mmrRankService.matchAcceptUserRank(count);
        }
        if(count == 13) {
            count = 0;
        }

        System.out.println("최종결과 : "+result.toString());
        
        return result;
    }


}
