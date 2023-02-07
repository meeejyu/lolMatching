package com.lol.match.main.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.lol.match.main.model.SettingDto;

@Mapper
public interface MainMapper {

    SettingDto findBySettingId();

}
