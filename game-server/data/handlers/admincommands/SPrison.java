package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author lord_rex 命令: //sprison <玩家名> <延迟>(分钟) 此命令将玩家发送至监狱。
 */
public class SPrison extends AdminCommand {

	public SPrison() {
		super("sprison", "将玩家发送至监狱.");
		setSyntaxInfo("<玩家名> <延迟分钟> <原因> - 示例：//sprison PlayerName 30 扰乱游戏秩序.");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1 || "help".equals(params[0])) {
			sendInfo(admin);
			return;
		}

		if (params.length < 3) {
			sendInfo(admin);
			return;
		}

		try {
			Player playerToPrison = World.getInstance().getPlayer(Util.convertName(params[0]));
			int delay = Integer.parseInt(params[1]);

			String reason = Util.convertName(params[2]);
			for (int itr = 3; itr < params.length; itr++)
				reason += " " + params[itr];

			if (playerToPrison != null) {
				PunishmentService.setIsInPrison(playerToPrison, true, delay, reason);
				PacketSendUtility.sendMessage(admin, "玩家 " + playerToPrison.getName() + " 已被送入监狱，时长 " + delay + " 分钟，原因：" + reason + "。");
			} else {
				PacketSendUtility.sendMessage(admin, "未找到玩家：" + params[0]);
			}
		} catch (Exception e) {
			sendInfo(admin);
		}

	}

	// @Override
	// public void info(Player player, String message) {
	// 	sendInfo(player);
	// }

	// private void sendInfo(Player player) {
	// 	PacketSendUtility.sendMessage(player, "语法 //sprison <玩家名> <延迟分钟> <原因>");
	// 	PacketSendUtility.sendMessage(player, "使用示例：//sprison PlayerName 30 扰乱游戏秩序");
	// }
}