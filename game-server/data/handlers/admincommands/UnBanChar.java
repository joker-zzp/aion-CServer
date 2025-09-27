package admincommands;

import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除角色封禁命令类 - 允许管理员解除对玩家角色的封禁
 * @author nrg
 */
public class UnBanChar extends AdminCommand {

  public UnBanChar() {
    super("unbanchar", "解除对玩家角色的封禁");
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params == null || params.length < 1) {
      PacketSendUtility.sendMessage(admin, "命令格式: //unbanchar <玩家名>");
      return;
    }
    if (params[0].equalsIgnoreCase("help")) {
      info(admin, null);
      return;
    }

    // Banned player must be offline
    String name = Util.convertName(params[0]);
    int playerId = PlayerDAO.getPlayerIdByName(name);
    if (playerId == 0) {
      PacketSendUtility.sendMessage(admin, "未找到玩家 " + name + "！");
      PacketSendUtility.sendMessage(admin, "命令格式: //unbanchar <玩家名>");
      return;
    }

    PacketSendUtility.sendMessage(admin, "角色 " + name + " 的封禁已解除！");

    PunishmentService.unbanChar(playerId);
  }

  @Override
  public void info(Player player, String message) {
    PacketSendUtility.sendMessage(player, "命令格式: //unbanchar <玩家名>");
    PacketSendUtility.sendMessage(player, "解除指定玩家角色的封禁");
  }
}