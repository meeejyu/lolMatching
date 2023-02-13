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
        
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

        RedisOperations<String, Object> operations = redisTemplate.opsForList().getOperations();
        
        if(operations.opsForList().size("teamList") > 0) {
            List<Object> queueList = operations.opsForList().range("teamList", 0, operations.opsForList().size("teamList")-1);

            for (int i = 0; i < queueList.size(); i++) {
                if(hashOperations.size("match:"+queueList.get(i).toString()) == 0) {
                    operations.opsForList().leftPop("teamList");
                }
                else {
                    break;
                }
            }
        }
    }
}
