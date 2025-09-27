package com.aionemu.gameserver.services.abyss;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.AbyssRankDAO.RankingListPlayerGp;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.aionemu.gameserver.world.World;

/**
 * @author ATracer, Neon
 */
public class AbyssRankUpdateService {

  private static final Logger log = LoggerFactory.getLogger(AbyssRankUpdateService.class);

  private AbyssRankUpdateService() {
  }

  public static void scheduleUpdate() {
    log.info("Scheduling ranking update task based on cron expression: " + RankingConfig.TOP_RANKING_UPDATE_RULE);
    CronService.getInstance().schedule(AbyssRankUpdateService::performUpdate, RankingConfig.TOP_RANKING_UPDATE_RULE, true);

    log.info("Scheduling daily GP loss task based on cron expression: " + RankingConfig.TOP_RANKING_DAILY_GP_LOSS_TIME);
    CronService.getInstance().schedule(AbyssRankUpdateService::updateDailyGpLoss, RankingConfig.TOP_RANKING_DAILY_GP_LOSS_TIME, true);
  }

  /**
   * Perform update of all ranks
   */
  public static void performUpdate() {
    ThreadPoolManager.getInstance().schedule(() -> {
      log.info("AbyssRankUpdateService: 执行更新排名...");
      long startTime = System.currentTimeMillis();

      // update and store rank statistics for all online players (offline players update on login)
      World.getInstance().forEachPlayer(player -> {
        player.getAbyssRank().doUpdate();
        AbyssRankDAO.storeAbyssRank(player);
      });
      // 根据排名模式选择不同的逻辑来计算minRank和playerLimit
      AbyssRankEnum minGpRank;
      int playerLimit;
      
      if (RankingConfig.isApOnlyMode()) {
        // AP模式下，从最低级别的军官开始
        minGpRank = AbyssRankEnum.STAR1_OFFICER;
        // 使用所有军官级别的最大配额
        playerLimit = Arrays.stream(AbyssRankEnum.values())
          .filter(rank -> rank.getId() >= AbyssRankEnum.STAR1_OFFICER.getId())
          .mapToInt(AbyssRankEnum::getQuota)
          .sum();
      } else {
        // 原有的GP模式逻辑
        minGpRank = Arrays.stream(AbyssRankEnum.values())
          .filter(rank -> rank.getRequiredGP() > 0)
          .min(Comparator.comparingInt(AbyssRankEnum::getId)).orElse(AbyssRankEnum.STAR1_OFFICER);
        playerLimit = Arrays.stream(AbyssRankEnum.values())
          .filter(rank -> rank.getRequiredGP() > 0)
          .mapToInt(AbyssRankEnum::getQuota)
          .max().orElse(1000);
      }
      // AbyssRankEnum minGpRank = Arrays.stream(AbyssRankEnum.values())
      //     .filter(rank -> rank.getRequiredGP() > 0)
      //     .min(Comparator.comparingInt(AbyssRankEnum::getId)).orElse(AbyssRankEnum.STAR1_OFFICER);
      // int playerLimit = Arrays.stream(AbyssRankEnum.values())
      //     .filter(rank -> rank.getRequiredGP() > 0)
      //     .mapToInt(AbyssRankEnum::getQuota)
      //     .max().orElse(1000);

      // update and store player & legion DB rank_pos entries (for ▼/▲/= trend in the ranking table)
      AbyssRankDAO.updateRankingLists(RankingConfig.TOP_RANKING_MAX_OFFLINE_DAYS, playerLimit, RankingConfig.RANKING_LIST_LEGION_LIMIT);
      // update and store GP ranks
      updateQuotaRanksForRace(Race.ASMODIANS, minGpRank);
      updateQuotaRanksForRace(Race.ELYOS, minGpRank);

      // update ranking cache
      AbyssRankingCache.getInstance().reloadRankings();

      log.info("AbyssRankUpdateService: Finished in " + (System.currentTimeMillis() - startTime) / 1000 + "s");
    }, 1000);
  }

  /**
   * Update player ranks based on quota for all players (online/offline)
   *  @param race
   *          the race that will be updated
   * @param minRank
   *          the minimum rank that will be updated (all lower GP ranks are disabled)
   */
  private static void updateQuotaRanksForRace(Race race, AbyssRankEnum minRank) {
    List<RankingListPlayerGp> rankingListSorted = AbyssRankDAO.loadRankingListPlayersGp(race);
    if (rankingListSorted == null)
      return;

    // 计算并设置新的排名
    int usedQuota = 0;
    for (int i = AbyssRankEnum.SUPREME_COMMANDER.getId(); i >= minRank.getId(); i--) {
      usedQuota = selectAndUpdateQuotaRank(AbyssRankEnum.getRankById(i), rankingListSorted, usedQuota);
    }

    // 设置所有不在排名列表中的玩家的军衔
    Map<Integer, Integer> pointsByPlayerId;
    if (RankingConfig.isApOnlyMode()) {
      // 如果是AP模式，即使有排名的玩家也要根据AP重新计算
      pointsByPlayerId = AbyssRankDAO.loadApOfPlayersNotInRankingList(race, minRank);
    } else {
      // 原有的GP模式逻辑
      pointsByPlayerId = AbyssRankDAO.loadApOfPlayersNotInRankingList(race, minRank);
    }
    
    if (pointsByPlayerId != null)
      updateToNoQuotaRank(pointsByPlayerId);

    // calculate and set new GP ranks
    // int usedQuota = 0;
    // for (int i = AbyssRankEnum.SUPREME_COMMANDER.getId(); i >= minRank.getId(); i--)
    //   usedQuota = selectAndUpdateQuotaRank(AbyssRankEnum.getRankById(i), rankingListSorted, usedQuota);

    // set all players with an old GP rank to AP ranks if they hold no rank position anymore
    // Map<Integer, Integer> apByPlayerId = AbyssRankDAO.loadApOfPlayersNotInRankingList(race, minRank);
    // if (apByPlayerId != null)
    //   updateToNoQuotaRank(apByPlayerId);
  }

  private static int selectAndUpdateQuotaRank(AbyssRankEnum rank, List<RankingListPlayerGp> rankingList, int usedQuota) {
    // 获取当前军衔的配额
    int currentRankQuota = rank.getQuota();

    for (Iterator<RankingListPlayerGp> iterator = rankingList.iterator(); iterator.hasNext();) {
      RankingListPlayerGp rankingListEntry = iterator.next();
      // 根据排名模式选择不同的判断条件
      if (RankingConfig.isApOnlyMode()) {
        // AP模式下同时检查配额和AP要求
        if (usedQuota >= currentRankQuota || 
            rankingListEntry.gp() < rank.getRequiredAP() ||
            (currentRankQuota > 0 && rankingListEntry.position() > (usedQuota + currentRankQuota))) {
            break;
        }
      } else {
        // GP模式下检查配额和GP要求
        if (usedQuota >= rank.getQuota() || rankingListEntry.gp() < rank.getRequiredGP()) {
          break;
        }
      }
      // if (usedQuota >= rank.getQuota() || rankingListEntry.gp() < rank.getRequiredGP())
      //   break;

      // 移除玩家并更新其排名
      iterator.remove();
      updateRankTo(rank, rankingListEntry.playerId(), rankingListEntry.position());
      usedQuota++;
    }
    return usedQuota;
  }

  private static void updateToNoQuotaRank(Map<Integer, Integer> pointsByPlayerId) {
    pointsByPlayerId.forEach((playerId, points) -> {
      AbyssRankEnum rank = AbyssRankEnum.getRankForPoints(points, RankingConfig.isApOnlyMode() ? Integer.MAX_VALUE : 0);
        updateRankTo(rank, playerId, 0);
    });

    // apByPlayerId.forEach((playerId, ap) -> {
    //   AbyssRankEnum rank = AbyssRankEnum.getRankForPoints(ap, 0); // no GP -> no officer ranks
    //   updateRankTo(rank, playerId, 0);
    // });
  }

  private static void updateRankTo(AbyssRankEnum newRank, int playerId, int rankingPosition) {
    // check if rank has changed for online players
    Player player = World.getInstance().getPlayer(playerId);
    if (player != null) {
      boolean rankChanged = player.getAbyssRank().getRank() != newRank;
      if (rankChanged) {
        player.getAbyssRank().setRank(newRank);
        // save to db now, so cache reload loads the correct rank for online players
        AbyssRankDAO.updateAbyssRank(playerId, newRank);
      }
      AbyssPointsService.onRankChanged(player, false, rankChanged, rankingPosition);
    } else {
      AbyssRankDAO.updateAbyssRank(playerId, newRank);
    }
  }

  private static void updateDailyGpLoss() {
    for (AbyssRankEnum rank : AbyssRankEnum.values()) {
      if (rank.getGpLossPerDay() > 0)
        AbyssRankDAO.dailyUpdateGp(rank);
    }
    for (Player p : World.getInstance().getAllPlayers()) {
      if (p.getAbyssRank().getRank().getGpLossPerDay() > 0) {
        GloryPointsService.decreaseGpBy(p.getObjectId(), p.getAbyssRank().getRank().getGpLossPerDay());
      }
    }
  }
}