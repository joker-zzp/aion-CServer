package admincommands;

import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author nrg
 */
public class BanChar extends AdminCommand {

	public BanChar() {
		super("banchar", "封禁玩家");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 3) {
			sendInfo(admin, true);
			return;
		}

		int playerId = 0;
		String playerName = Util.convertName(params[0]);

		// First, try to find player in the World
		Player player = World.getInstance().getPlayer(playerName);
		if (player != null)
			playerId = player.getObjectId();

		// Second, try to get player Id from offline player from database
		if (playerId == 0)
			playerId = PlayerDAO.getPlayerIdByName(playerName);

		// Third, fail
		if (playerId == 0) {
			PacketSendUtility.sendMessage(admin, "玩家 " + playerName + " 未找到!");
			sendInfo(admin, true);
			return;
		}

		int dayCount = -1;
		try {
			dayCount = Integer.parseInt(params[1]);
		} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "第二个参数不是整数");
				sendInfo(admin, true);
				return;
			}

		if (dayCount < 0) {
			PacketSendUtility.sendMessage(admin, "第二个参数必须是正数天数或0(永久)");
			sendInfo(admin, true);
			return;
		}

		String reason = Util.convertName(params[2]);
		for (int itr = 3; itr < params.length; itr++)
			reason += " " + params[itr];

		PacketSendUtility.sendMessage(admin, "角色 " + playerName + " 已被禁封 " + dayCount + " 天!");

		PunishmentService.banChar(playerId, dayCount, reason);
	}

	@Override
	public void info(Player player, String message) {
		sendInfo(player, false);
	}

	private void sendInfo(Player player, boolean withNote) {
		PacketSendUtility.sendMessage(player, "用法: //banChar <玩家名> <天数>/0 (永久) <原因>");
		if (withNote)
			PacketSendUtility.sendMessage(player, "注意: 即使今天只剩下几个小时，当前日期也被视为一整天!");
	}
}