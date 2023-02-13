package com.lol.match;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.Test;

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




    }
}
