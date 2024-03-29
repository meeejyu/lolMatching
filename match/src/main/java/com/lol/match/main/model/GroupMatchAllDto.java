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
public class GroupMatchAllDto {

    private String userInfo;

    private List<UserAllDto> teamAList;

    private List<UserAllDto> teamBList;

}
