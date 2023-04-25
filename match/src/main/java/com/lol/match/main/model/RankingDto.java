package com.lol.match.main.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RankingDto {
    
    private int rankingId;

    private String rankingName;

    private int rankingLevel;

}
