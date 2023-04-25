package com.lol.match.main.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.lol.match.main.model.PositionDto;
import com.lol.match.main.model.RankingDto;
import com.lol.match.main.model.SettingDto;
import com.lol.match.main.model.UserAllDto;
import com.lol.match.main.model.UserMmrDto;
import com.lol.match.main.model.UserPositionDto;
import com.lol.match.main.model.UserRankingDto;

@Mapper
public interface MainMapper {

    SettingDto findBySettingAllId();

    SettingDto findBySettingMmrId();

    SettingDto findBySettingPositionId();

    SettingDto findBySettingRankingId();

    UserAllDto findByAllUserId(@Param("userId")int userId);

    UserRankingDto findByRankingUserId(@Param("userId")int userId);

    UserPositionDto findByPositionUserId(@Param("userId")int userId);

    UserMmrDto findByMmrUserId(@Param("userId")int userId);

    List<RankingDto> findByRanking();

    RankingDto findByRankingId(@Param("rankingId")int rankingId);

    List<PositionDto> findByPosition();

    PositionDto findByPositionId(@Param("positionId")int positionId);

}
