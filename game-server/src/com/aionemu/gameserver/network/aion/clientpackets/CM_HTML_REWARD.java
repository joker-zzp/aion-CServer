package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.HTMLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 处理HTML奖励领取请求的客户端数据包
 * 包括每日登录奖励等HTML弹窗中的奖励领取
 */
public class CM_HTML_REWARD extends AionClientPacket {
  private static final Logger log = LoggerFactory.getLogger(CM_HTML_REWARD.class);
  
  private int messageId;
  private List<Integer> itemIds;
  
  /**
   * @param opcode
   * @param validStates
   */
  public CM_HTML_REWARD(int opcode, Set<State> validStates) {
    super(opcode, validStates);
  }
  
  @Override
  protected void readImpl() {
    messageId = readD(); // 消息ID
    int count = readH(); // 物品数量
    
    itemIds = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      itemIds.add(readD()); // 物品ID
    }
  }
  
  @Override
  protected void runImpl() {
    Player player = getConnection().getActivePlayer();
    if (player == null) {
      return;
    }
    
    try {
      // 检查是否是每日登录奖励请求（我们使用特殊标记-1）
      // 由于在daily_login_reward.xhtml中只有一个选项，所以这里简化处理
      if (itemIds != null && !itemIds.isEmpty()) {
        // 对于每日登录奖励，我们使用特殊标记-1
        List<Integer> rewardItems = new ArrayList<>();
        rewardItems.add(-1); // 特殊标记
        
        // 调用HTMLService处理奖励领取
        HTMLService.getReward(player, messageId, rewardItems);
      }
    } catch (Exception e) {
      log.error("Error processing HTML reward request: {}", e.getMessage());
    }
  }
}