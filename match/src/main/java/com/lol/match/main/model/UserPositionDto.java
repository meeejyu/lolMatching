package com.lol.match.main.model;

import lombok.Data;

@Data
public class UserPositionDto {

    private String userId;

    private int userMmr;

    private String userNickname;

    private String userPosition;

    private String queueName;

    // 큐 이름 세팅
    public void queueNameSet(String queueName) {
        this.queueName = queueName;
    }
    
}
