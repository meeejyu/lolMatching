package com.lol.match.main.model;

import lombok.Data;

@Data
public class UserMmrDto {
    
    private int userId;

    private int userMmr;

    private String userNickname;

    private String teamName;

    // 큐 이름 세팅
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
