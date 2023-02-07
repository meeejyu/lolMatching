package com.lol.match.domain.dto;

import java.util.List;

import com.lol.match.main.model.UserMatchDto;

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
