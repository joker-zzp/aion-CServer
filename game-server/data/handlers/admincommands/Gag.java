package admincommands;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.ban.ChatBanService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Watson, Neon
 */
public class Gag extends AdminCommand {

	public Gag() {
		super("gag", "禁止玩家使用所有聊天功能.");

		setSyntaxInfo(
			"<player> <duration> <reason> - 禁止指定玩家在特定时间内使用聊天功能(单位: 分钟).",
			"<player> <remove> - 解除该玩家的聊天禁令."
		);
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		Player player = World.getInstance().getPlayer(Util.convertName(params[0]));
		if (player == null || !player.isOnline()) {
			PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_MSG_ASK_PCINFO_LOGOFF());
			return;
		}

		if (params.length < 2) {
			sendInfo(admin);
			return;
		}

		if (params[1].equalsIgnoreCase("remove")) {
			if (!ChatBanService.isBanned(player)) {
				sendInfo(admin, "玩家 " + player.getName() + " 已经可以聊天.");
				return;
			}

			ChatBanService.unbanPlayer(player);
			sendInfo(admin, "已解除玩家 " + player.getName() + " 的禁言状态.");
			return;
		}

		int time = 0;
		try {
			time = Integer.valueOf(params[1]);
		} catch (NumberFormatException e) {
			sendInfo(admin, "<duration> 必须是整数(时间单位: 分钟).");
			return;
		}

		if (time < 1) {
			sendInfo(admin, "<duration> 必须至少为1分钟。");
			return;
		}

		if (params.length < 3 || params[2].trim().length() <= 1) {
			sendInfo(admin, "必须指定 <reason> (禁言原因).");
			return;
		}

		ChatBanService.banPlayer(player, time * 60000);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_INGAME_BLOCK_ENABLE_NO_CHAT(time));

		String reason = StringUtils.join(params, ' ', 2, params.length);
		sendInfo(player, StringUtils.appendIfMissing(StringUtils.capitalize(reason), ".", "!"));
		sendInfo(admin, "玩家 " + player.getName() + " 已被禁言 " + time + " 分钟.");
	}
}