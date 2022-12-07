package com.lol.matching.dto;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcmDto {
	@NotEmpty(message = "ecmId값이 빈값입니다.")
	private String ecmId;

	private int test;
}

