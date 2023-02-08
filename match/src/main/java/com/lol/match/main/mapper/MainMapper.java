package com.lol.match.main.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lol.match.main.model.PositionDto;
import com.lol.match.main.model.RankDto;
import com.lol.match.main.model.SettingAllDto;
import com.lol.match.main.model.UserAllDto;
import com.lol.match.main.model.UserPositionDto;
import com.lol.match.main.model.UserRankDto;

@Mapper
public interface MainMapper {

    SettingAllDto findBySettingId();

    UserAllDto findByAllUserId(@Param("userId")int userId);

    UserPositionDto findByPositionUserId(@Param("userId")int userId);

    UserRankDto findByRankUserId(@Param("userId")int userId);

    List<RankDto> findByRank();

    List<PositionDto> findByPositionId(@Param("positionId")int positionId);

}
