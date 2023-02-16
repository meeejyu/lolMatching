package com.lol.match.main.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lol.match.main.model.PositionDto;
import com.lol.match.main.model.RankDto;
import com.lol.match.main.model.SettingDto;
import com.lol.match.main.model.UserAllDto;
import com.lol.match.main.model.UserMmrDto;
import com.lol.match.main.model.UserPositionDto;
import com.lol.match.main.model.UserRankDto;

@Mapper
public interface MainMapper {

    SettingDto findBySettingAllId();

    SettingDto findBySettingMmrId();

    SettingDto findBySettingPositionId();

    SettingDto findBySettingRankId();

    UserAllDto findByAllUserId(@Param("userId")int userId);

    UserRankDto findByRankUserId(@Param("userId")int userId);

    UserPositionDto findByPositionUserId(@Param("userId")int userId);

    UserMmrDto findByMmrUserId(@Param("userId")int userId);

    List<RankDto> findByRank();

    RankDto findByRankId(@Param("rankId")int rankId);

    List<PositionDto> findByPosition();

    PositionDto findByPositionId(@Param("positionId")int positionId);

}
