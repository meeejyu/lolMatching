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

    // 수락 시간 : 초 단위, 0일 경우 수락시간 무제한
    private int settingTime;

    // 팀 배정 타입
    private String settingType;

}
