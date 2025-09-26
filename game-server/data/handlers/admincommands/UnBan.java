package admincommands;

import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除封禁命令类 - 允许管理员解除对玩家的账号、IP或全部封禁
 * @author Watson
 */
public class UnBan extends AdminCommand {

	public UnBan() {
		super("unban", "解除对玩家的账号、IP或全部封禁");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "命令格式: //unban <玩家名> [account|ip|full]");
			return;
		}
		if (params[0].equalsIgnoreCase("help")) {
			info(admin, null);
			return;
		}

		// Banned player must be offline, so get his account ID from database
		String name = Util.convertName(params[0]);
		int accountId = PlayerDAO.getAccountIdByName(name);
		if (accountId == 0) {
			PacketSendUtility.sendMessage(admin, "未找到玩家 " + name + "！");
			PacketSendUtility.sendMessage(admin, "命令格式: //unban <玩家名> [account|ip|full]");
			return;
		}

		byte type = 3; // Default: full
		if (params.length > 1) {
			// Smart Matching
			String stype = params[1].toLowerCase();
			if (("account").startsWith(stype))
				type = 1;
			else if (("ip").startsWith(stype))
				type = 2;
			else if (("full").startsWith(stype))
				type = 3;
			else {
				PacketSendUtility.sendMessage(admin, "命令格式: //unban <玩家名> [account|ip|full]");
				return;
			}
		}

		// Sends time -1 to unban
		LoginServer.getInstance().sendBanPacket(type, accountId, "", -1, admin.getObjectId());
	}

	@Override
	public void info(Player player, String message) {
		PacketSendUtility.sendMessage(player, "命令格式: //unban <玩家名> [account|ip|full]");
		PacketSendUtility.sendMessage(player, "account - 只解除账号封禁");
		PacketSendUtility.sendMessage(player, "ip - 只解除IP封禁");
		PacketSendUtility.sendMessage(player, "full - 同时解除账号和IP封禁(默认)");
	}
}