package com.lol.match.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserMatchDto {

    // private int userMatchId;
    
    private String userId;

    private int mmr;

    private String userNickname;

    private String position;

    private String rank;

    private int time; // 24시에서 0시로 넘어갈때를 잘 고려해야함.

    // private String groupId;

    // private int index; // 해당 그룹에 들어온 순서, 0~9번째까지
    // 9번이 생기면 그 팀은 매칭 완료가 된상태로 간주
    // 그룹이 존재하지 않으면 무조건 0번   
}
