package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Elusive, Neon
 */
public class Kick extends AdminCommand {

  public Kick() {
    super("kick", "将玩家从服务器断开连接。");

    // @formatter:off
    setSyntaxInfo(
      "<名称> - 将指定名称的玩家断开连接。",
      "<ALL> - 断开所有玩家的连接(为安全起见，参数必须大写)。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    if ("ALL".equals(params[0])) {
      if (World.getInstance().getAllPlayers().size() == 1) {
        sendInfo(admin, "当前没有在线玩家可踢出。");
        return;
      }
      World.getInstance().forEachPlayer(player -> {
        if (!player.equals(admin)) {
          player.getClientConnection().close(SM_SYSTEM_MESSAGE.STR_KICK_CHARACTER());
          PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_USER_KICKED(player.getName()));
        }
      });
    } else {
      Player player = World.getInstance().getPlayer(Util.convertName(params[0]));
      if (player == null) {
        PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_BUDDYLIST_NO_OFFLINE_CHARACTER());
        return;
      }
      player.getClientConnection().close(SM_SYSTEM_MESSAGE.STR_KICK_CHARACTER());
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_USER_KICKED(player.getName()));
    }
  }
}