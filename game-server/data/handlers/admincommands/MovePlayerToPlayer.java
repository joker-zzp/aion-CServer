package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * Admin moveplayertoplayer command.
 * 
 * @author Tanelorn
 */
public class MovePlayerToPlayer extends AdminCommand {

  public MovePlayerToPlayer() {
    super("moveplayertoplayer", "将一个玩家传送到另一个玩家所在位置。");
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params == null || params.length < 2) {
      PacketSendUtility.sendMessage(admin, "语法: //moveplayertoplayer <要移动的角色名> <目标角色名>");
      return;
    }

    Player playerToMove = World.getInstance().getPlayer(Util.convertName(params[0]));
    if (playerToMove == null) {
      PacketSendUtility.sendMessage(admin, "指定的玩家不在线。");
      return;
    }

    Player playerDestination = World.getInstance().getPlayer(Util.convertName(params[1]));
    if (playerDestination == null) {
      PacketSendUtility.sendMessage(admin, "目标玩家不在线。");
      return;
    }

    if (playerToMove.equals(playerDestination)) {
      PacketSendUtility.sendMessage(admin, "无法将玩家移动到自己所在的位置。");
      return;
    }

    TeleportService.teleportTo(playerToMove, playerDestination.getWorldId(), playerDestination.getInstanceId(), playerDestination.getX(),
      playerDestination.getY(), playerDestination.getZ(), playerDestination.getHeading());

    PacketSendUtility.sendMessage(admin, "已将玩家 " + playerToMove.getName() + " 传送到玩家 " + playerDestination.getName() + " 的位置。");
    PacketSendUtility.sendMessage(playerToMove, "您已被管理员传送。");
  }

  @Override
  public void info(Player player, String message) {
    PacketSendUtility.sendMessage(player, "语法: //moveplayertoplayer <要移动的角色名> <目标角色名>");
  }
}