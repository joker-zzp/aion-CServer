package playercommands;

import static com.aionemu.gameserver.custom.instance.CustomInstanceService.REWARD_COIN_ID;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * @author Estrayl
 */
public class Buy extends PlayerCommand {

  private static final Map<Integer, Map<String, Integer>> rewards = new LinkedHashMap<>();

  public Buy() {
    super("buy", "使用你的 " + ChatUtil.item(REWARD_COIN_ID) + " 兑换各种奖励.");

    // @formatter:off
    setSyntaxInfo(
      " - 显示所有可购买的奖励.",
      "<物品链接|ID> - 购买相应的物品."
    );
    // @formatter:on
    initRewards();
  }

  private void initRewards() {
    addReward(166000194, 5, 40); // 德尔塔强化石
    addReward(166000195, 5, 50); // 艾普西隆强化石
    addReward(188051516, 1, 100); // 智能高级卷轴包
    addReward(162000107, 30, 100); // 萨阿姆王的草药
    addReward(162000124, 50, 100); // 高级恢复药水
    addReward(188051868, 1, 50); // [活动] 最强因坤形态糖果袋
    addReward(188053610, 3, 150); // [活动] 70级合成魔石包
    addReward(188053618, 1, 200); // 荣耀的伊利姆的伊迪安包
    addReward(166500002, 2, 50); // 增幅石
    addReward(166030005, 2, 80); // 淬炼溶液
    addReward(188053295, 1, 200); // 天国羽毛宝箱
    addReward(188950015, 1, 400); // 特殊快递通行证 (永恒/61-65级)
    addReward(188950019, 1, 500); // 特殊快递通行证 (神话/61-65级)
    addReward(165020015, 1, 1000); // 防具包裹卷轴 (永恒/65级及以下)
    addReward(165020014, 1, 1150); // 武器包裹卷轴 (永恒/65级及以下)
    addReward(165020021, 1, 1350); // 贵族防具包裹卷轴 (神话/65级及以下)
    addReward(165020020, 1, 1500); // 贵族武器包裹卷轴 (神话/65级及以下)
    addReward(190020175, 1, 1500); // 塔哈巴塔蛋
    addReward(187060103, 1, 1300); // 威望之翼
    addReward(169610342, 1, 1000); // [称号] 被遗忘的征服者
    addReward(190100051, 1, 1200); // 飞行帕加蒂
    addReward(188053109, 1, 2500); // 阿什里翁装备宝箱
  }

  @Override
  protected void execute(Player player, String... params) {
    if (params.length == 0)
      showRewards(player);
    else if (params.length == 1)
      buyItem(player, params[0]);
    else
      sendInfo(player);
  }

  private void showRewards(Player player) {
    sendInfo(player, "价格:");
    for (Entry<Integer, Map<String, Integer>> idMap : rewards.entrySet()) {
      String itemString;
      String cost = String.valueOf(idMap.getValue().get("cost"));
      int amount = idMap.getValue().get("amount");
      int digitsToPad = 4 - cost.length();
      if (digitsToPad > 0)
        cost = StringUtils.repeat(' ', digitsToPad * 2) + cost;

      if (amount == 1)
        itemString = cost + "   ";
      else
        itemString = cost + "   " + amount + "x ";

      itemString += ChatUtil.item(idMap.getKey());
      sendInfo(player, itemString);
    }
    sendInfo(player, "购买方法:");
    sendInfo(player, "1. 右键点击列表中的物品添加到记事本.");
    sendInfo(player, "2. 在聊天框中输入 " + ChatUtil.color(getAliasWithPrefix(), Color.WHITE) + " (带空格).");
    sendInfo(player, "3. (Ctrl + 右键) 从记事本中点击物品.");
  }

  private void buyItem(Player player, String itemLink) {
    // 兑换物品
    int itemId = ChatUtil.getItemId(itemLink);
    if (DataManager.ITEM_DATA.getItemTemplate(itemId) == null) {
      sendInfo(player, "\"" + itemLink + "\" 不是有效的物品 (必须是物品链接或ID).");
      return;
    }

    if (!rewards.containsKey(itemId)) {
      sendInfo(player, ChatUtil.item(itemId) + " 不在奖励列表中.");
      return;
    }
    int cost = rewards.get(itemId).get("cost");
    long rewardCoins = 0;
    for (Item i : player.getInventory().getItemsByItemId(REWARD_COIN_ID))
      rewardCoins += i.getItemCount();

    if (cost > rewardCoins) {
      sendInfo(player, "购买 " + ChatUtil.item(itemId) + " 需要 " + cost + ".");
      return;
    }

    RequestResponseHandler<Creature> handler = new RequestResponseHandler<Creature>(null) {

      @Override
      public void acceptRequest(Creature requester, Player responder) {
        if (player.getInventory().decreaseByItemId(REWARD_COIN_ID, cost)) {
          sendInfo(player, "你花费了 " + cost + ".");
          ItemService.addItem(player, itemId, rewards.get(itemId).get("amount"), true,
            new ItemUpdatePredicate(ItemAddType.DECOMPOSABLE, ItemUpdateType.INC_CASH_ITEM));
        }
      }
    };
    if (player.getResponseRequester().putRequest(SM_QUESTION_WINDOW.STR_AIONJEWEL_SHOP_BUY_CONFIRM, handler))
      PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_AIONJEWEL_SHOP_BUY_CONFIRM, 0, 0, cost,
        DataManager.ITEM_DATA.getItemTemplate(REWARD_COIN_ID).getL10n()));
  }

  private void addReward(int itemID, int amount, int cost) {
    Map<String, Integer> val = new LinkedHashMap<>();
    val.put("cost", cost);
    val.put("amount", amount);
    rewards.put(itemID, val);
  }
}
