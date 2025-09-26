package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.panesterra.PanesterraService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Yeats, Neon
 */
public class Ahserion extends AdminCommand {

	public Ahserion() {
		super("ahserion", "启动或停止Ahserion的飞行");

		// @formatter:off
		setSyntaxInfo(
			"<start> - 启动Ahserion的飞行",
			"<stop> - 停止Ahserion的飞行"
		);
		// @formatter:on
	}

	@Override
	protected void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		if (params[0].equalsIgnoreCase("start")) {
			if (PanesterraService.getInstance().isAhserionRaidStarted()) {
				sendInfo(admin, "Ahserion的飞行已经在运行");
			} else {
				PanesterraService.getInstance().startAhserionRaid();
				sendInfo(admin, "已启动Ahserion的飞行");
			}
		} else if (params[0].equalsIgnoreCase("stop")) {
			if (!PanesterraService.getInstance().isAhserionRaidStarted()) {
				sendInfo(admin, "Ahserion的飞行未运行");
			} else {
				PanesterraService.getInstance().stopAhserionRaid();
				sendInfo(admin, "已停止Ahserion的飞行");
			}
		}
	}
}