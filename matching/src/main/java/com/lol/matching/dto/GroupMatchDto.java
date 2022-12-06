package com.lol.matching.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupMatchDto {
    
    private String groupId;

    private int max;

    private int min;

}
