package admincommands;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RESURRECT;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Sarynth
 */
public class Res extends AdminCommand {

	public Res() {
		super("res", "复活玩家。");
		// @formatter:off
		setSyntaxInfo(
			"<prompt> - 向目标玩家发送复活请求。",
			"<instant> - 立即复活目标玩家。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		final VisibleObject target = admin.getTarget();
		if (target == null) {
			PacketSendUtility.sendMessage(admin, "未选择目标。");
			return;
		}

		if (!(target instanceof Player)) {
			PacketSendUtility.sendMessage(admin, "你只能复活其他玩家。");
			return;
		}

		final Player player = (Player) target;
		if (!player.isDead()) {
			PacketSendUtility.sendMessage(admin, "该玩家已经是活着的状态。");
			return;
		}

		// 默认操作是发送复活请求。
		if (params == null || params.length == 0 || "help".equals(params[0]) || ("prompt").startsWith(params[0])) {
			if (params != null && params.length > 0 && "help".equals(params[0])) {
				sendInfo(admin);
				return;
			}
			player.setPlayerResActivate(true);
			PacketSendUtility.sendPacket(player, new SM_RESURRECT(admin));
			return;
		}

		if (("instant").startsWith(params[0])) {
			PlayerReviveService.skillRevive(player);
			return;
		}

		PacketSendUtility.sendMessage(admin, "[复活] 用法：选择玩家目标并使用 //res <instant|prompt>");
	}

	@Override
	public void info(Player player, String message) {
		// TODO Auto-generated method stub
	}
}