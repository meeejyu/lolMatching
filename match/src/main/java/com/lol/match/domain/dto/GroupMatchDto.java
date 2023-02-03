package com.lol.match.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMatchDto {

    private String groupName;

    private String userId;

    private int mmr;

    private String userNickname;

    private String position;

}
