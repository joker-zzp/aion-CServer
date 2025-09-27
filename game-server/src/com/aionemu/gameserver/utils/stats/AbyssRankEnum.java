package com.aionemu.gameserver.utils.stats;

import javax.xml.bind.annotation.XmlEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ChatUtil;

/**
 * @author ATracer, Sarynth, Imaginary
 */
@XmlEnum
public enum AbyssRankEnum {
  GRADE9_SOLDIER(1, 300, 90, 0, 0),
  GRADE8_SOLDIER(2, 345, 103, 1000, 0),
  GRADE7_SOLDIER(3, 396, 118, 2850, 0),
  GRADE6_SOLDIER(4, 455, 136, 5422, 0),
  GRADE5_SOLDIER(5, 523, 156, 9331, 0),
  GRADE4_SOLDIER(6, 601, 180, 15713, 0),
  GRADE3_SOLDIER(7, 721, 216, 26669, 0),
  GRADE2_SOLDIER(8, 865, 259, 46089, 0),
  GRADE1_SOLDIER(9, 1038, 311, 81165, 0),
  STAR1_OFFICER(10, 1557, 467, 244222, 1244),
  STAR2_OFFICER(11, 1868, 560, 476720, 1368),
  STAR3_OFFICER(12, 2148, 644, 936087, 1915),
  STAR4_OFFICER(13, 2470, 741, 1844652, 3064),
  STAR5_OFFICER(14, 3705, 1482, 3642632, 5210),
  GENERAL(15, 4075, 1630, 9336260, 8335),
  GREAT_GENERAL(16, 4482, 1792, 18846985, 10002),
  COMMANDER(17, 4930, 1972, 38057630, 11503),
  SUPREME_COMMANDER(18, 5916, 2366, 76862114, 12437);

  // 添加静态日志对象
  private static final Logger log = LoggerFactory.getLogger(AbyssRankEnum.class);

  private final int id;
  private final int pointsGained;
  private final int pointsLost;
  private final int requiredAP;
  private final int requiredGP;

  // 新添加的字段，用于存储计算后的AP值
  private int calculatedAP;
  // 是否启用动态计算AP
  private static boolean useDynamicAPCalculation = false;
  // 基础AP值
  private static int BASE_AP = 1000;
  // private static double growthRate = 0.4; // 基础增长率
  private static double soldierGrowthRate = 0.3; // 士兵阶段增长率
  private static double officerGrowthRate = 0.5; // 军官阶段增长率
  private static double generalGrowthRate = 0.7; // 将军阶段增长率

  AbyssRankEnum(int id, int pointsGained, int pointsLost, int required, int gloryPointsRequired) {
    this.id = id;
    this.pointsGained = pointsGained;
    this.pointsLost = pointsLost;
    this.requiredAP = required;
    this.requiredGP = gloryPointsRequired;

    // 初始化时计算AP
    calculateRequiredAP();
  }

  // 新增方法动态计算AP
  private void calculateRequiredAP() {
    if (!useDynamicAPCalculation) {
      this.calculatedAP = requiredAP;
      return;
    }
    
    double rate = 0;

    // 根据不同等级阶段设置增长率
    if (id > 14) {
      // 将军阶段
      rate = generalGrowthRate;
    } else if (id > 9) {
      // 军官阶段
      rate = officerGrowthRate;
    } else {
      // 士兵阶段
      rate = soldierGrowthRate;
    }

    // 对待初级最低等级为 0
    if (id == 1) {
      this.calculatedAP = 0;
    } else if (id == 2) { 
      // 等级2 使用基础AP值
      this.calculatedAP = BASE_AP;
    } else {
      // 基于前一等级的AP值计算
      AbyssRankEnum prevRank = values()[id - 2];
      // 计算当前等级所需AP 并四舍五入
      this.calculatedAP = (int) Math.round(prevRank.calculatedAP * (1 + rate));
    }
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @return the pointsLost
   */
  public int getPointsLost() {
    return pointsLost;
  }

  /**
   * @return the pointsGained
   */
  public int getPointsGained() {
    return pointsGained;
  }

  /**
   * @return AP required for Rank
   */
  public int getRequiredAP() {
    // return requiredAP;
    // 动态计算AP
    return calculatedAP;
  }

  // 新增静态方法，用于从配置文件加载参数
  public static void initializeFromConfig() {
    // 从RankingConfig读取参数
    useDynamicAPCalculation = RankingConfig.DYNAMIC_AP_CALCULATION;
    // growthRate = RankingConfig.DYNAMIC_AP_GROWTH_RATE;
    soldierGrowthRate = RankingConfig.DYNAMIC_AP_SOLDIER_GROWTH_RATE;
    officerGrowthRate = RankingConfig.DYNAMIC_AP_OFFICER_GROWTH_RATE;
    generalGrowthRate = RankingConfig.DYNAMIC_AP_GENERAL_GROWTH_RATE;

    // 重新计算所有等级的AP值
    for (AbyssRankEnum rank : values()) {
      rank.calculateRequiredAP();
      // debug 输出
      log.info("AP 等级 {} 计算后的AP值 {}", rank.getId(), rank.getRequiredAP());
    }
  }

  public int getRequiredGP() {
    return requiredGP;
  }

  public int getGpLossPerDay() {
    return RankingConfig.TOP_RANKING_GP_LOSS.getOrDefault(this, 0);
  }

  /**
   * @return The quota is the maximum number of allowed player to have the rank
   */
  public int getQuota() {
    return RankingConfig.TOP_RANKING_QUOTA.getOrDefault(this, 0);
  }

  public static String getRankL10n(Player player) {
    return player.getAbyssRank().getRank().getRankL10n(player.getRace());
  }

  public static String getRankL10n(Race race, int rankId) {
    return getRankById(rankId).getRankL10n(race);
  }

  public String getRankL10n(Race race) {
    int rank9L10nId = race == Race.ELYOS ? 901215 : 901233;
    int rankL10nId = rank9L10nId + ordinal();
    return ChatUtil.l10n(rankL10nId);
  }

  public static AbyssRankEnum getRankById(int id) {
    for (AbyssRankEnum rank : values()) {
      if (rank.getId() == id)
        return rank;
    }
    throw new IllegalArgumentException("Invalid abyss rank provided " + id);
  }

  public static AbyssRankEnum getRankForPoints(int ap, int gp) {
    AbyssRankEnum r = AbyssRankEnum.GRADE9_SOLDIER;
    for (AbyssRankEnum rank : values()) {
      if (RankingConfig.isApOnlyMode()) {
        // 在AP模式下，仅考虑AP要求
        if (rank.getRequiredAP() <= ap) {
          r = rank;
        }
      } else {
        // 在GP模式下，同时考虑AP和GP要求
        if (rank.getRequiredAP() <= ap && rank.getRequiredGP() <= gp) {
          r = rank;
        }
      }
    }
    return r;
  }
}