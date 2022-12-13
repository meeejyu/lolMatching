package com.lol.matching.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lol.matching.aws.AwsSqsCreate;
import com.lol.matching.aws.AwsSqsRead;
import com.lol.matching.dto.UserMatchDto;
import com.lol.matching.service.MainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@Controller
public class MainController {
    
    List<String> list = new ArrayList<>();
    int listSize = 0;

    private final MainService mainService;

    private final AwsSqsCreate awsSqsCreate;

    private final AwsSqsRead awsSqsRead;

    @GetMapping(value="/create")
    @ResponseBody
    public String create(@RequestBody UserMatchDto userMatchDto) {

        String queueName = awsSqsCreate.createQueue(userMatchDto);
        mainService.sendMessage(userMatchDto, queueName);
        awsSqsRead.readMessage(queueName);

        System.out.println("큐 만들어짐 확인 바람");
        return "OK";
    }

    @GetMapping(value="/getList")
    @ResponseBody
    public String getList(@RequestBody UserMatchDto userMatchDto) {

        awsSqsCreate.getQueues(userMatchDto);

        return "OK";
    }

    @GetMapping(value="/read")
    @ResponseBody
    public String read() {

        awsSqsRead.readMessage("bronze_150_250");

        // System.out.println("메세지 확인 바람");
        return "OK";
    }

    @GetMapping(value="/write")
    @ResponseBody
    public String write(@RequestBody UserMatchDto userMatchDto) {

        // bronze_150_250
        mainService.sendMessage(userMatchDto, "bronze_150_250");

        // System.out.println("메세지 확인 바람");
        return "OK";
    }

    @PostMapping("/match")
    @ResponseBody
    public String match(@RequestBody UserMatchDto userMatchDto) throws JsonProcessingException {
        // 유저 아이디 중복 안되게 막기, 두번 요청은 안됨
        
        // mainService.sendMessage(userMatchDto);

        return "OK";
    }

    @GetMapping(value="/main")
    @ResponseBody
    public String main() {

        // amazonSQSSender2.test();
        System.out.println("메인을 스쳐지나감");
        // return "test.html";
        return "OK";
    }

    @GetMapping(value="/test")
    public @ResponseBody String test1(String a) {

        if(a.isBlank()) {
            return "fail";
        }
        String last = "";
    
        list.add(a);
        listSize = listSize +1;
        System.out.println(list.toString());

        if(list.size() >= 3 ) {
            last = a;
        }

        Boolean bol = false;       
        check(a, bol);

        return "success";
    }

    public void check(String a, Boolean bol) {
        if(listSize>=3) {
            System.out.println("가라 : "+a);
            zero(a);
            System.out.println("끝 : "+ a);
            bol = true;
            return;
        }
        else {
            if(bol) {
                return;
            }
            else {
                while(listSize<3) {
                    try {
                        System.out.println("너 슬립중이니?" + a);
                        Thread.sleep(1000);
                        check(a, bol);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void zero(String a) {
        System.out.println("제로에 온 순서는? "+ a );
        if(list.get(0).equals(a)) {
            System.out.println(a+" 통과 ");
            list.remove(0);
            return;
        }
        else {
            try {
                System.out.println("제로 슬립중이니?" + a);
                Thread.sleep(1000);
                zero(a);                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
