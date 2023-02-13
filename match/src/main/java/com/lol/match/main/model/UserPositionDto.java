package com.lol.match.main.model;

import lombok.Data;

@Data
public class UserPositionDto {

    private int userId;

    private int userMmr;

    private String userNickname;

    private int positionId;

    private String teamName;

    // 큐 이름 세팅
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    
}
