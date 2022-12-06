package com.lol.matching.dto;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserMatchDto {
    
    private String userId;

    private String userNickname;

    private GroupMatchDto groupMatchDto;
    
}
