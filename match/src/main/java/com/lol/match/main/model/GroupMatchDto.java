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
public class GroupMatchDto {

    private String userInfo;

    private List<UserMatchDto> teamAList;

    private List<UserMatchDto> teamBList;

}
