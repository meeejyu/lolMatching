package com.lol.matching.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;



@Controller
public class MainController {
    
    List<String> list = new ArrayList<>();
    int listSize = 0;

    @GetMapping(value="/main")
    public String main() {

        System.out.println("메인을 스쳐지나감");
        return "test.html";
    }

    @GetMapping(value="/test")
    public @ResponseBody String test1(String a) {

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
