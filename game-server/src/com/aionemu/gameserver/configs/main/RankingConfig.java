package com.aionemu.gameserver.configs.main;

import java.util.Map;

import org.quartz.CronExpression;

import com.aionemu.commons.configuration.Properties;
import com.aionemu.commons.configuration.Property;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * @author Sarynth
 */
public class RankingConfig {

	@Property(key = "gameserver.topranking.updaterule", defaultValue = "0 0 0 ? * *")
	public static CronExpression TOP_RANKING_UPDATE_RULE;

	@Property(key = "gameserver.topranking.daily.gploss.time", defaultValue = "0 0 12 ? * *")
	public static CronExpression TOP_RANKING_DAILY_GP_LOSS_TIME;

	@Property(key = "gameserver.topranking.legion_limit", defaultValue = "50")
	public static int RANKING_LIST_LEGION_LIMIT;

	@Property(key = "gameserver.topranking.max.offline.days", defaultValue = "0")
	public static int TOP_RANKING_MAX_OFFLINE_DAYS;

	@Property(key = "gameserver.topranking.xform.min_rank", defaultValue = "STAR5_OFFICER")
	public static AbyssRankEnum XFORM_MIN_RANK;

	@Properties(keyPattern = "^gameserver\\.topranking\\.quota\\.(.+)")
	public static Map<AbyssRankEnum, Integer> TOP_RANKING_QUOTA;

	@Properties(keyPattern = "^gameserver\\.topranking\\.gp_loss\\.(.+)")
	public static Map<AbyssRankEnum, Integer> TOP_RANKING_GP_LOSS;

	// 在现有配置后添加
	@Property(key = "gameserver.topranking.mode", defaultValue = "GP_BASED")
	public static String RANKING_MODE;
	
	// 添加一个辅助方法来检查是否为AP_ONLY模式
	public static boolean isApOnlyMode() {
		return "AP_ONLY".equalsIgnoreCase(RANKING_MODE);
	}
	
	// 动态深渊点数(AP)计算配置
	@Property(key = "gameserver.abyss.ap.dynamic_calculation", defaultValue = "false")
	public static boolean DYNAMIC_AP_CALCULATION;
	
	// 移除不使用的基础增长率配置
	// @Property(key = "gameserver.abyss.ap.growth_rate", defaultValue = "0.4")
	// public static double DYNAMIC_AP_GROWTH_RATE;
	
	@Property(key = "gameserver.abyss.ap.soldier_growth_rate", defaultValue = "0.3")
	public static double DYNAMIC_AP_SOLDIER_GROWTH_RATE;
	
	@Property(key = "gameserver.abyss.ap.officer_growth_rate", defaultValue = "0.5")
	public static double DYNAMIC_AP_OFFICER_GROWTH_RATE;
	
	@Property(key = "gameserver.abyss.ap.general_growth_rate", defaultValue = "0.7")
	public static double DYNAMIC_AP_GENERAL_GROWTH_RATE;

}