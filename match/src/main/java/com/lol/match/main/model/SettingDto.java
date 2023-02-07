package com.lol.match.main.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class SettingDto {
    
    private int settingId;

    // 매칭 지표
    private int settingMmr;

    // 정원
    private int settingHeadcount;

    // 수락 시간 : 초 단위
    private int settingTime;

    private int positionId;

    private String position1;

    private String position2;

    private String position3;

    private String position4;

    private String position5;

    private int rankId;

    private String rank1;

    private String rank2;
    
    private String rank3;
    
    private String rank4;
    
    private String rank5;
    
    private String rank6;
    
    private String rank7;
    
    private String rank8;
    
    private String rank9;
    
    private String rank10;

}
