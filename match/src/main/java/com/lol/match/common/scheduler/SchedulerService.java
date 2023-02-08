package com.lol.match.common.scheduler;

import java.util.List;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    @Scheduled(cron="0 0/1 * * * *")
    public void listDeleteScheduler() {

        log.info("리스트 삭제 작업 실행");
        Boolean condition = true;
        
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        
        if(operations.opsForList().size("queueList") > 0) {
            List<Object> queueList = operations.opsForList().range("queueList", 0, operations.opsForList().size("queueList")-1);

            int count = 0;

            while(condition) {
                if(hashOperations.size("map:"+queueList.get(count).toString()) == 0) {
                    operations.opsForList().leftPop("queueList");
                    count =+ 1;
                    if(operations.opsForList().size("queueList") == 0) {
                        condition = false;
                    }
                }
                else {
                    condition = false;
                }
            }

        }
    }
}
