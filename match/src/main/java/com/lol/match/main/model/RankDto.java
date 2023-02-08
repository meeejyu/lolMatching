package com.lol.match.main.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RankDto {
    
    private int rankId;

    private String rankName;

    private int rankLevel;

}
