package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除MAC地址封禁命令类 - 允许管理员解除对指定MAC地址的封禁
 * @author KID
 */
public class UnBanMac extends AdminCommand {

	public UnBanMac() {
		super("unbanmac", "解除对指定MAC地址的封禁");
	}

	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			info(player, null);
			return;
		}
		if (params[0].equalsIgnoreCase("help")) {
			info(player, null);
			return;
		}

		String address = params[0];
		boolean result = BannedMacManager.getInstance().unbanAddress(address,
			"uban;mac=" + address + ", " + player.getObjectId() + "; admin=" + player.getName());
		if (result)
			PacketSendUtility.sendMessage(player, "MAC地址 " + address + " 的封禁已解除");
		else
			PacketSendUtility.sendMessage(player, "MAC地址 " + address + " 未被封禁");
	}

	@Override
	public void info(Player player, String message) {
		PacketSendUtility.sendMessage(player, "命令格式: //unbanmac <MAC地址>");
		PacketSendUtility.sendMessage(player, "解除对指定MAC地址的封禁");
		PacketSendUtility.sendMessage(player, "示例: //unbanmac 00:11:22:33:44:55");
	}
}