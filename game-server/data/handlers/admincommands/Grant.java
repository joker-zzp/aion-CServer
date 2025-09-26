package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Neon
 */
public class Grant extends AdminCommand {

	public Grant() {
		super("grant", "授予/撤销账户权限。");

		// @formatter:off
		setSyntaxInfo(
				"<a> <level> [name] - 授予指定的访问级别 (默认: 目标账户，可选: 指定角色账户). 0将移除账户的访问级别.",
				"<m> <level> [name] - 授予指定的会员级别 (默认: 目标账户，可选: 指定角色账户). 0将移除账户的会员级别."
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 2) {
			sendInfo(admin);
			return;
		}

		int type;
		if ("a".equalsIgnoreCase(params[0])) {
			type = 1;
		} else if ("m".equalsIgnoreCase(params[0])) {
			type = 2;
		} else {
			sendInfo(admin);
			return;
		}

		int level = Integer.parseInt(params[1]);
		if (level < 0) {
			sendInfo(admin, "级别不能为负数.");
			return;
		}

		Player player;
		if (params.length >= 3) {
			String playerName = Util.convertName(params[2]);
			player = World.getInstance().getPlayer(playerName);
			if (player == null) {
				PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(playerName));
				return;
			}
		} else if (admin.getTarget() instanceof Player target) {
			player = target;
		} else {
			PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
			return;
		}
		if (type == 1) {
			if (!player.equals(admin) && player.getAccount().getAccessLevel() >= admin.getAccount().getAccessLevel()) {
				sendInfo(admin, "你不允许修改拥有相同或更高访问级别的玩家的访问级别,");
				return;
			}
		}

		LoginServer.getInstance().sendLsControlPacket(type, level, player, admin);
	}
}