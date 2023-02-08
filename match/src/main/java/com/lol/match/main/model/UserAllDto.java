package com.lol.match.main.model;

import lombok.Data;

@Data
public class UserAllDto {

    private int userId;

    private int userMmr;

    private String userNickname;

    private int rankId;

    private int positionId;

    private String queueName;

    // 큐 이름 세팅
    public void queueNameSet(String queueName) {
        this.queueName = queueName;
    }
    
}
