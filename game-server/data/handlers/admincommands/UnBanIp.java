package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除IP封禁命令类 - 允许管理员解除对指定IP地址的封禁
 * @author Watson
 */
public class UnBanIp extends AdminCommand {

	public UnBanIp() {
		super("unbanip", "解除对指定IP地址的封禁");
	}

	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "命令格式: //unbanip <IP掩码>");
			return;
		}
		if (params[0].equalsIgnoreCase("help")) {
			info(player, null);
			return;
		}

		LoginServer.getInstance().sendBanPacket((byte) 2, 0, params[0], -1, player.getObjectId());
	}

	@Override
	public void info(Player player, String message) {
		PacketSendUtility.sendMessage(player, "命令格式: //unbanip <IP掩码>");
		PacketSendUtility.sendMessage(player, "解除对指定IP地址或IP范围的封禁");
		PacketSendUtility.sendMessage(player, "示例: //unbanip 192.168.1.1 或 //unbanip 192.168.1.0/24");
	}
}