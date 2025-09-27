package admincommands;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

public class Invasion extends AdminCommand {

  private static final String COMMAND_START = "start";
  private static final String COMMAND_STOP = "stop";

  public Invasion() {
    super("invasion", "控制世界入侵事件。");
  }

  @Override
  public void execute(Player player, String... params) {

    if (params.length == 0) {
      showHelp(player);
      return;
    }

    if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
      handleStartStopInvasion(player, params);
    }
  }

  protected void handleStartStopInvasion(Player player, String... params) {
    if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
      showHelp(player);
      return;
    }

    int vortexId = NumberUtils.toInt(params[1]);
    String locationName = vortexId == 0 ? "狄奥波墨斯" : "布鲁斯特豪宁";
    if (!isValidVortexLocationId(player, vortexId)) {
      showHelp(player);
      return;
    }

    if (COMMAND_START.equalsIgnoreCase(params[0])) {
      if (VortexService.getInstance().isInvasionInProgress(vortexId)) {
        PacketSendUtility.sendMessage(player, locationName + " 已经处于被围攻状态。");
      } else {
        PacketSendUtility.sendMessage(player, locationName + " 入侵开始！");
        VortexService.getInstance().startInvasion(vortexId);
      }
    } else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
      if (!VortexService.getInstance().isInvasionInProgress(vortexId)) {
        PacketSendUtility.sendMessage(player, locationName + " 未处于被围攻状态。");
      } else {
        PacketSendUtility.sendMessage(player, locationName + " 入侵已停止！");
        VortexService.getInstance().stopInvasion(vortexId);
      }
    }
  }

  protected boolean isValidVortexLocationId(Player player, int vortexId) {

    if (!VortexService.getInstance().getVortexLocations().keySet().contains(vortexId)) {
      PacketSendUtility.sendMessage(player, "ID " + vortexId + " 无效。");
      return false;
    }

    return true;
  }

  protected void showHelp(Player player) {
    PacketSendUtility.sendMessage(player, "管理员命令 //invasion start|stop <ID>");
  }

}