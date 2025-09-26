package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GAME_TIME;
import com.aionemu.gameserver.services.GameTimeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.time.gametime.GameTime;

/**
 * 游戏时间控制命令类 - 允许管理员更改游戏内的时间设置
 * @author Pan, Neon, Sykra
 */
public class Time extends AdminCommand {

	public Time() {
		super("time", "更改游戏内时间");

		// @formatter:off
		setSyntaxInfo(
				"help - 显示时间命令的帮助信息",
				"<dawn|day|dusk|night> - 设置指定的时间段",
				"<0-23> - 设置指定的小时",
				"<0-23> <0-59> - 设置指定的小时和分钟"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}
		if (params[0].equalsIgnoreCase("help")) {
			sendInfo(admin);
			return;
		}
		int hour;
		int minute = 0;
		if (params[0].equalsIgnoreCase("night")) {
			hour = 22;
		} else if (params[0].equalsIgnoreCase("dusk")) {
			hour = 18;
		} else if (params[0].equalsIgnoreCase("day")) {
			hour = 9;
		} else if (params[0].equalsIgnoreCase("dawn")) {
			hour = 4;
		} else {
			try {
				hour = Integer.parseInt(params[0]);
				if (hour < 0 || hour > 23)
					throw new IllegalArgumentException("一天只有24小时！\n最小值: 0 - 最大值: 23");
				if (params.length == 2) {
					minute = Integer.parseInt(params[1]);
					if (minute < 0 || minute > 59)
						throw new IllegalArgumentException("一小时只有60分钟！\n最小值: 0 - 最大值: 59");
				}
			} catch (IllegalArgumentException e) {
				sendInfo(admin, e.getClass() == IllegalArgumentException.class ? e.getMessage() : null); // default info for NumberFormatException
				return;
			}
		}

		GameTime gameTime = GameTimeService.getInstance().getGameTime();
		int hourOffset = hour - gameTime.getHour(); // hour offset inside the same day
		int minutesToAdd = 60 * hourOffset;
		if (minute == 0) {
			minutesToAdd -= gameTime.getMinute();
		} else {
			int minuteOffset = minute - gameTime.getMinute();
			minutesToAdd += minuteOffset;
		}
		gameTime.addMinutes(minutesToAdd);
		PacketSendUtility.broadcastToWorld(new SM_GAME_TIME());
		sendInfo(admin, "时间已更改为 " + gameTime.getHour() + ":" + String.format("%02d", gameTime.getMinute()) + "。");
	}
}