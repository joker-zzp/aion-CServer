package admincommands;

import java.util.NoSuchElementException;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author lord_rex 命令: //rprison <玩家名> 此命令用于将玩家从监狱中释放。
 */
public class RPrison extends AdminCommand {

  public RPrison() {
    super("rprison", "将玩家从监狱中释放.");
    setSyntaxInfo("<玩家名> - 示例：//rprison PlayerName");
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length < 1 || params.length > 2 || (params.length > 0 && "help".equals(params[0]))) {
      sendInfo(admin);
      return;
    }

    try {
      Player playerFromPrison = World.getInstance().getPlayer(Util.convertName(params[0]));

      if (playerFromPrison != null) {
        PunishmentService.setIsInPrison(playerFromPrison, false, 0, "");
        PacketSendUtility.sendMessage(admin, "玩家 " + playerFromPrison.getName() + " 已从监狱中释放。");
      }
    } catch (NoSuchElementException nsee) {
      sendInfo(admin);
    } catch (Exception e) {
      sendInfo(admin);
    }
  }

  // @Override
  // public void info(Player player, String message) {
  //   PacketSendUtility.sendMessage(player, "语法: //rprison <玩家名>");
  // }
}