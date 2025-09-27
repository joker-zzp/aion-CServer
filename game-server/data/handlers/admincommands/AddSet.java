package admincommands;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.itemset.ItemPart;
import com.aionemu.gameserver.model.templates.itemset.ItemSetTemplate;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Antivirus
 */
public class AddSet extends AdminCommand {

  public AddSet() {
    super("addset", "为玩家添加物品");

    setSyntaxInfo(
      "<物品id> - 为选择目标添加物品",
      "[玩家名] [物品套装ID] - 为玩家添加物品"
    );
  }

  @Override
  public void execute(Player player, String... params) {
    if (params.length == 0 || params.length > 2) {
      sendInfo(player);
      return;
    }

    int itemSetId = 0;
    Player receiver = null;

    try {
      itemSetId = Integer.parseInt(params[0]);
      receiver = player;
    } catch (NumberFormatException e) {
      receiver = World.getInstance().getPlayer(Util.convertName(params[0]));

      if (receiver == null) {
        PacketSendUtility.sendMessage(player, "无法找到该名称的玩家");
        return;
      }

      try {
        itemSetId = Integer.parseInt(params[1]);
      } catch (NumberFormatException ex) {

        PacketSendUtility.sendMessage(player, "物品套装ID必须是数字");
        return;
      } catch (Exception ex2) {
        PacketSendUtility.sendMessage(player, "发生错误");
        return;
      }
    }

    ItemSetTemplate itemSet = DataManager.ITEM_SET_DATA.getItemSetTemplate(itemSetId);
    if (itemSet == null) {
      PacketSendUtility.sendMessage(player, "不存在ID为" + itemSetId + "的物品套装");
      return;
    }

    if (receiver.getInventory().getFreeSlots() < itemSet.getItempart().size()) {
      PacketSendUtility.sendMessage(player, "背包至少需要" + itemSet.getItempart().size() + "个空位");
      return;
    }

    for (ItemPart setPart : itemSet.getItempart()) {
      long count = ItemService.addItem(receiver, setPart.getItemId(), 1);

      if (count != 0) {
        PacketSendUtility.sendMessage(player, "物品" + setPart.getItemId() + "无法添加");
        return;
      }
    }

    PacketSendUtility.sendMessage(player, "物品套装添加成功");
    PacketSendUtility.sendMessage(receiver, "管理员给了你一套物品");
  }

  @Override
  public void info(Player player, String message) {
    PacketSendUtility.sendMessage(player, "语法 //addset <玩家> <物品套装ID>");
    PacketSendUtility.sendMessage(player, "语法 //addset <物品套装ID>");
  }

}