package playercommands;

import com.aionemu.gameserver.custom.pvpmap.PvpMapService;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * @author Yeats
 */
public class Pvp extends PlayerCommand {

  public Pvp() {
    super("pvp", "加入自定义PvP地图, 您可以在此与对立阵营战斗.");

    setSyntaxInfo(
      "join - 加入自定义PvP地图",
      "leave - 离开自定义PvP地图",
      "info - 显示当前地图上的玩家数量"
    );
  }

  @Override
  public void execute(Player player, String... params) {
    if (params.length == 0) {
      sendInfo(player);
    } else if (params.length >= 1) {
      if (params[0].equalsIgnoreCase("join")) {
        PvpMapService.getInstance().joinMap(player);
      } else if (params[0].equalsIgnoreCase("leave")) {
        PvpMapService.getInstance().leaveMap(player);
      } else if (params[0].equalsIgnoreCase("info")) {
        int size = PvpMapService.getInstance().getParticipantsSize();
        sendInfo(player, "当前地图上" + (size == 0 ? "没有" : (size == 1 ? "有" + size + "名" : "有" + size + "名")) + "玩家。");
      } else {
        sendInfo(player);
      }
    }
  }
}
