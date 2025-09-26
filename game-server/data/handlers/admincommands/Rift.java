package admincommands;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

public class Rift extends AdminCommand {

	private static final String COMMAND_OPEN = "open";
	private static final String COMMAND_CLOSE = "close";

	public Rift() {
		super("rift", "控制裂隙的开启和关闭。");
		// @formatter:off
		setSyntaxInfo(
				"<open> <ID|世界ID> <是否有守卫> - 开启指定ID的裂隙（守卫参数为true/false）。",
				"<close> <ID|世界ID> - 关闭指定ID的裂隙。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {

		if (params.length == 0 || "help".equals(params[0])) {
			showHelp(player);
			return;
		}

		if (COMMAND_CLOSE.equalsIgnoreCase(params[0]) || COMMAND_OPEN.equalsIgnoreCase(params[0])) {
			handleRift(player, params);
		}
	}

	protected void handleRift(Player player, String... params) {
		if (params.length < 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}

		int id = NumberUtils.toInt(params[1]);
		boolean result;
		if (!isValidId(player, id)) {
			showHelp(player);
			return;
		}

		if (COMMAND_OPEN.equalsIgnoreCase(params[0])) {
			boolean guards = Boolean.parseBoolean(params[2]);
			result = RiftService.getInstance().openRifts(id, guards);
			PacketSendUtility.sendMessage(player, result ? "裂隙已开启！" : "裂隙已经处于开启状态");
		} else if (COMMAND_CLOSE.equalsIgnoreCase(params[0])) {
			result = RiftService.getInstance().closeRifts(id);
			PacketSendUtility.sendMessage(player, result ? "裂隙已关闭！" : "裂隙已经处于关闭状态");
		}
	}

	protected boolean isValidId(Player player, int id) {
		if (!RiftService.getInstance().isValidId(id)) {
			PacketSendUtility.sendMessage(player, "ID " + id + " 无效");
			return false;
		}

		return true;
	}

	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "管理命令 //rift open|close <ID|世界ID> (开启时指定守卫参数为true/false)");
	}

}