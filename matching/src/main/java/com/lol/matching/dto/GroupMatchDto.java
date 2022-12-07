package com.lol.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMatchDto {

    // private int groupMatchId;
    
    private String groupId;

    private int max;

    private int min;

}
