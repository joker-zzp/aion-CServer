package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.TemporaryPlayerTeam;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Cyrakuse, Estrayl
 */
public class MoveToMe extends AdminCommand {

  public MoveToMe() {
    super("movetome", "将玩家（可选其队伍）传送到您的位置。");
    // @formatter:off
    setSyntaxInfo(
      "<名称> - 仅传送该玩家。",
      "<名称> <(g)rp|(a)lli> - 传送该玩家及其所在队伍或联盟。");
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length < 1) {
      sendInfo(admin);
      return;
    }
    String playerName = Util.convertName(params[0]);
    Player playerToMove = World.getInstance().getPlayer(playerName);
    if (playerToMove == null) {
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(playerName));
      return;
    }
    if (params.length >= 2) {
      if (!playerToMove.isInTeam()) {
        sendInfo(admin, "该玩家不属于任何队伍。");
        return;
      }
      TemporaryPlayerTeam<?> teamToMove;
      switch (params[1].toLowerCase()) {
        case "g":
        case "grp":
        case "group":
          teamToMove = playerToMove.getPlayerGroup();
          break;
        case "a":
        case "alli":
        case "alliance":
          teamToMove = playerToMove.getCurrentTeam();
          break;
        default:
          sendInfo(admin);
          return;
      }
      if (teamToMove == null) {
          sendInfo(admin, playerToMove.getName() + " 当前没有队伍。");
          return;
        }
      teamToMove.getOnlineMembers().forEach(p -> teleportPlayer(p, admin));
    } else {
      teleportPlayer(playerToMove, admin);
    }
  }

  private void teleportPlayer(Player playerToMove, Player admin) {
    TeleportService.teleportTo(playerToMove, admin.getPosition());
    sendInfo(admin, "已将 " + playerToMove.getName() + " 传送到您的位置。");
    sendInfo(playerToMove, "您已被 " + admin.getName() + " 传送。");
  }
}