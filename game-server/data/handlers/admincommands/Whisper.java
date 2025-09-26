package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.CustomPlayerState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 私聊控制命令类 - 允许管理员启用或禁用接收私聊消息
 */
public class Whisper extends AdminCommand {

	public Whisper() {
		super("whisper", "启用/禁用接收私聊消息");

		setSyntaxInfo(
			"<help> - 显示私聊命令的帮助信息。",
			"<on|off> - 启用或禁用接收其他玩家的私聊消息（GM始终可以向你发送私聊）。"
		);
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
		} else if (params[0].equalsIgnoreCase("off")) {
			admin.setCustomState(CustomPlayerState.NO_WHISPERS_MODE);
			sendInfo(admin, "接收私聊消息: 关闭");
		} else if (params[0].equalsIgnoreCase("on")) {
			admin.unsetCustomState(CustomPlayerState.NO_WHISPERS_MODE);
			sendInfo(admin, "接收私聊消息: 开启");
		}
	}
}