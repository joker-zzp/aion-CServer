package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CubeExpandService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Kamui
 */
public class AddCube extends AdminCommand {

	public AddCube() {
		super("addcube", "添加玩家9格背包空间");
	}

	@Override
	public void execute(Player admin, String... params) {

		if (params.length != 1) {
			PacketSendUtility.sendMessage(admin, "语法 //addcube <玩家名称>");
			return;
		}

		Player receiver = null;

		receiver = World.getInstance().getPlayer(Util.convertName(params[0]));

		if (receiver == null) {
			PacketSendUtility.sendMessage(admin, "玩家" + Util.convertName(params[0]) + "不在线");
			return;
		}

		if (CubeExpandService.canExpand(receiver)) {
			CubeExpandService.npcExpand(receiver);
			PacketSendUtility.sendMessage(admin, "已成功为玩家" + receiver.getName() + "添加9格背包空间");
			PacketSendUtility.sendMessage(receiver, "管理员" + admin.getName() + "为你扩展了背包空间");
		} else {
			PacketSendUtility.sendMessage(admin, "无法为玩家" + receiver.getName() + "添加背包扩展\n原因 该玩家背包已完全扩展");
			return;
		}
	}

	@Override
	public void info(Player admin, String message) {
		PacketSendUtility.sendMessage(admin, "语法 //addcube <玩家名称>");
	}
}