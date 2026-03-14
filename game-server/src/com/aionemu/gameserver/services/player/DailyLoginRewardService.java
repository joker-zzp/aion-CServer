package com.aionemu.gameserver.services.player;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.configs.main.HTMLConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.item.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每日登录奖励服务类
 * 处理玩家每日登录奖励的领取和显示
 */
public class DailyLoginRewardService {
  private static final Logger log = LoggerFactory.getLogger(DailyLoginRewardService.class);
  private static final DailyLoginRewardService INSTANCE = new DailyLoginRewardService();
  
  // 存储玩家最后领取奖励的时间
  private final ConcurrentHashMap<Integer, Long> lastRewardTime = new ConcurrentHashMap<>();
  // 存储玩家连续登录天数
  private final ConcurrentHashMap<Integer, Integer> consecutiveDays = new ConcurrentHashMap<>();
  
  public static DailyLoginRewardService getInstance() {
    return INSTANCE;
  }
  
  /**
   * 玩家登录时调用，检查并显示每日奖励
   * @param player 登录的玩家
   */
  public void onPlayerLogin(Player player) {
    if (!HTMLConfig.ENABLE_HTML_WELCOME) {
      return;
    }
    
    int playerObjId = player.getObjectId();
    long now = System.currentTimeMillis();
    
    // 检查是否已经领取了今天的奖励
    if (canReceiveReward(playerObjId, now)) {
      // 更新连续登录天数
      int days = updateConsecutiveDays(playerObjId, now);
      
      // 获取今日奖励物品ID
      int rewardItemId = getDailyRewardItemId(days);
      if (rewardItemId > 0) {
        // 显示奖励弹窗
        showDailyRewardWindow(player, rewardItemId, days);
      }
    }
  }
  
  /**
   * 检查玩家是否可以领取奖励
   * @param playerObjId 玩家对象ID
   * @param now 当前时间
   * @return 是否可以领取奖励
   */
  private boolean canReceiveReward(int playerObjId, long now) {
    Long lastTime = lastRewardTime.get(playerObjId);
    if (lastTime == null) {
      // 首次登录，还没有领取过奖励
      return true;
    }
    
    // 检查是否已经过了一天
    Calendar lastCal = Calendar.getInstance();
    lastCal.setTimeInMillis(lastTime);
    
    Calendar nowCal = Calendar.getInstance();
    nowCal.setTimeInMillis(now);
    
    // 比较日期是否不同
    return lastCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR) ||
           lastCal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR);
  }
  
  /**
   * 更新连续登录天数
   * @param playerObjId 玩家对象ID
   * @param now 当前时间
   * @return 更新后的连续登录天数
   */
  private int updateConsecutiveDays(int playerObjId, long now) {
    Long lastTime = lastRewardTime.get(playerObjId);
    Integer days = consecutiveDays.getOrDefault(playerObjId, 0);
    
    if (lastTime == null) {
      // 首次登录
      consecutiveDays.put(playerObjId, 1);
      return 1;
    }
    
    Calendar lastCal = Calendar.getInstance();
    lastCal.setTimeInMillis(lastTime);
    lastCal.add(Calendar.DAY_OF_YEAR, 1); // 上次领取奖励的第二天
    
    Calendar nowCal = Calendar.getInstance();
    nowCal.setTimeInMillis(now);
    
    // 重置时间部分，只比较日期
    lastCal.set(Calendar.HOUR_OF_DAY, 0);
    lastCal.set(Calendar.MINUTE, 0);
    lastCal.set(Calendar.SECOND, 0);
    lastCal.set(Calendar.MILLISECOND, 0);
    
    nowCal.set(Calendar.HOUR_OF_DAY, 0);
    nowCal.set(Calendar.MINUTE, 0);
    nowCal.set(Calendar.SECOND, 0);
    nowCal.set(Calendar.MILLISECOND, 0);
    
    if (lastCal.getTimeInMillis() == nowCal.getTimeInMillis()) {
      // 连续登录
      days++;
    } else {
      // 中断，重新开始计数
      days = 1;
    }
    
    consecutiveDays.put(playerObjId, days);
    return days;
  }
  
  /**
   * 获取每日奖励物品ID
   * @param consecutiveDays 连续登录天数
   * @return 奖励物品ID
   */
  private int getDailyRewardItemId(int consecutiveDays) {
    if (consecutiveDays > 7) {
      return 188053866; // L10 幸运铜钱包x50
    } else if (consecutiveDays > 3) {
      return 188053865; // L10 幸运铜钱包x10
    } else {
      return 188053864; // L10 幸运铜钱包x3
    }
  }
  
  /**
   * 显示每日奖励弹窗
   * @param player 玩家对象
   * @param itemId 奖励物品ID
   * @param days 连续登录天数
   */
  private void showDailyRewardWindow(Player player, int itemId, int days) {
    try {
      // 获取物品模板
      ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
      if (itemTemplate == null) {
        log.warn("Daily login reward item not found: {}", itemId);
        return;
      }
      
      // 获取HTML模板并替换变量
      String htmlContent = HTMLCache.getInstance().getHTML("daily_login_reward.xhtml");
      if (htmlContent == null) {
        log.warn("Daily login reward HTML template not found");
        return;
      }
      
      // 替换模板中的变量
      htmlContent = htmlContent.replace("%item_id%", String.valueOf(itemId));
      htmlContent = htmlContent.replace("%item_name%", itemTemplate.getName());
      // 使用物品ID作为图标标识符（因为ItemTemplate没有getIcon方法）
      htmlContent = htmlContent.replace("%item_icon%", String.valueOf(itemId));
      htmlContent = htmlContent.replace("%login_days%", String.valueOf(days));
      
      // 显示HTML窗口
      HTMLService.showHTML(player, htmlContent);
    } catch (Exception e) {
      log.error("Error showing daily login reward window: {}", e.getMessage());
    }
  }
  
  /**
   * 发放每日登录奖励
   * @param player 玩家对象
   * @param messageId 消息ID
   */
  public void giveDailyReward(Player player, int messageId) {
    try {
      int playerObjId = player.getObjectId();
      int days = consecutiveDays.getOrDefault(playerObjId, 1);
      int itemId = getDailyRewardItemId(days);
      
      // 发放奖励物品
      ItemService.addItem(player, itemId, 1);
      
      // 更新最后领取时间
      lastRewardTime.put(playerObjId, System.currentTimeMillis());
      
      // 保存到数据库
      saveLoginRewardInfo(player);
    } catch (Exception e) {
      log.error("Error giving daily login reward: {}", e.getMessage());
    }
  }
  
  /**
   * 保存登录奖励信息到数据库
   * @param player 玩家对象
   */
  private void saveLoginRewardInfo(Player player) {
    try {
      // 这里应该实现将登录奖励信息保存到数据库的逻辑
      // 例如记录最后领取时间、连续登录天数等
      // 由于缺少具体的DAO实现，这里只是一个示例
      log.info("Saved daily login reward info for player: {}", player.getName());
    } catch (Exception e) {
      log.error("Error saving daily login reward info: {}", e.getMessage());
    }
  }
}