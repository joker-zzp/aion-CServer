package admincommands;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Phantom, ATracer, Source
 */
public class Add extends AdminCommand {

  public Add() {
    super("add", "向玩家背包添加基纳或物品");

    // @formatter:off
    setSyntaxInfo(
      "kinah <数量> - 向自己背包添加指定数量的基纳",
      "<物品链接|ID> [数量] - 向自己背包添加指定物品",
      "<玩家> kinah <数量> - 向指定玩家背包添加基纳",
      "<玩家> <物品链接|ID> [数量] - 向指定玩家背包添加物品"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player player, String... params) {
    if (params.length < 1) {
      sendInfo(player);
      return;
    }

    int index = 0;
    Player receiver = player;
    int itemId = params.length == 2 && "Kinah".equalsIgnoreCase(params[index]) ? ItemId.KINAH : ChatUtil.getItemId(params[index]);
    if (itemId == 0) {
      String playerName = Util.convertName(params[index]);
      receiver = World.getInstance().getPlayer(playerName);
      if (receiver == null) {
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(playerName));
        return;
      }
      if (++index < params.length)
        itemId = "Kinah".equalsIgnoreCase(params[index]) ? ItemId.KINAH : ChatUtil.getItemId(params[index]);
    }

    ItemTemplate itemTemplate;
    if (itemId == 0 || (itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId)) == null) {
      sendInfo(player, "无效物品");
      return;
    }

    long itemCount = params.length > ++index ? Long.parseLong(params[index]) : 1;
    if (itemCount <= 0
      || (itemId == ItemId.KINAH ? receiver.getInventory().getKinah() + itemCount < 0 : itemCount / itemTemplate.getMaxStackCount() > 126)) {
      sendInfo(player, "无效物品数量");
      return;
    }

    if (!AdminService.getInstance().canOperate(player, receiver, itemId, "command //add"))
      return;

    long notAddedCount = ItemService.addItem(receiver, itemId, itemCount, true);
    if (notAddedCount == 0) {
      if (player != receiver) {
        sendInfo(player, "你为" + receiver.getName() + "添加了" + itemCount + " x [item:" + itemId + "]");
        sendInfo(receiver, "你从" + player.getName() + "收到" + itemCount + " x [item:" + itemId + "]");
      } else {
        sendInfo(player, "你添加了" + itemCount + " x [item:" + itemId + "]");
      }
    } else {
      sendInfo(player, "物品无法添加");
    }
  }
}