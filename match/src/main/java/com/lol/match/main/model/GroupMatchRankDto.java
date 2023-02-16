package com.lol.match.main.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMatchRankDto {

    private String userInfo;

    private List<UserRankDto> teamAList;

    private List<UserRankDto> teamBList;

}
