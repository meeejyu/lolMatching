package com.lol.match.main.controller;

import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lol.match.main.model.GroupMatchDto;
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

    // 매칭 하기 all 타입
    @PostMapping("/match")
    @ResponseBody
    public HashMap<String, String> match(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.matchUser(userId);
        
        return result;
    }


    // 유저가 대전을 찾는 와중 대전 찾기를 취소한 경우 : 팀에서 해당 유저 정보 삭제
    @PostMapping("/match/cancel")
    @ResponseBody
    public HashMap<String, String> teamListDelete(@RequestParam int userId) throws Exception {

        HashMap<String, String> result = mainService.teamListDeleteUser(userId);

        return result;
    }

    // 대전 매칭 완료 된 이후 수락하기 
    @PostMapping("/match/accept")
    @ResponseBody
    public HashMap<String, String> matchAccept(@RequestParam int userId) throws Exception {
        
        HashMap<String, String> result = mainService.matchAcceptUser(userId);

        return result;
    }

    // 대전 매칭 완료 후 팀 배정 정보 및 본인이 속한 팀 정보 주기
    @PostMapping("/match/complete")
    @ResponseBody
    public GroupMatchDto matchComplete(@RequestParam int userId, String teamName) throws Exception {
        
        GroupMatchDto groupMatchDto = mainService.matchCompleteUser(userId, teamName);
        
        return groupMatchDto;

    }
    // 게임이 종료된 이후 해당 메소드 실행
    @DeleteMapping("/match/end/{listName}")
    @ResponseBody
    public String matchEnd(@PathVariable String listName) {
        
        mainService.deleteMatchAll(listName);

        return "ok";
    }
    
    // 매칭이 완료됐지만 아무도 수락하지 않은 경우 해당 메소드 실행
    @DeleteMapping("/match/delete/{listName}")
    @ResponseBody
    public String delete(@PathVariable String listName) {
        
        mainService.deleteMatchInfo(listName);
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
        if(count < 41) {
            result = mainService.matchUser(count);
        }
        if(count == 41) {
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
        if(count < 41) {
            result = mainService.matchAcceptUser(count);
        }
        if(count == 41) {
            count = 0;
        }
        System.out.println("최종결과 : "+result.toString());
        
        return result;
    }


}
