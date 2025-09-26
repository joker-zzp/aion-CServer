package playercommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_CHANGE_ALLOWED_HDD_SERIAL;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * @author ViAl, Neon
 */
public class Lock extends PlayerCommand {

	public Lock() {
		super("lock", "启用/禁用阻止从其他计算机登录的功能.");

		setSyntaxInfo(
			"login - 锁定当前计算机登录",
			"level - 锁定当前等级"
		);
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			sendInfo(player);
			return;
		}

		String command = params[0].toLowerCase();

		switch (command) {
			case "login":
				lockLogin(player);
				break;
			case "level":
				lockLevel(player);
				break;
			default:
				sendInfo(player);
				break;
		}

		// if ("enable".equalsIgnoreCase(params[0])) {
		// 	String hddSerial = player.getClientConnection().getHddSerial();
		// 	if (hddSerial == null || hddSerial.isEmpty()) {
		// 		sendInfo(player, "Couldn't lock your account. Please re-log and try again.");
		// 		return;
		// 	}
		// 	player.getAccount().setAllowedHddSerial(hddSerial);
		// 	LoginServer.getInstance().sendPacket(new SM_CHANGE_ALLOWED_HDD_SERIAL(player.getAccount()));
		// 	sendInfo(player, "Your account is now locked. You will not be able to login from any other computer from now on.");
		// } else if ("disable".equalsIgnoreCase(params[0])) {
		// 	player.getAccount().setAllowedHddSerial(null);
		// 	LoginServer.getInstance().sendPacket(new SM_CHANGE_ALLOWED_HDD_SERIAL(player.getAccount()));
		// 	sendInfo(player, "Your account is unlocked. You can login from any computer again.");
		// } else {
		// 	sendInfo(player, "Invalid parameter.");
		// }
	}

	// 锁定解锁 异地登录
	private void lockLogin(Player player) {
		String hddSerial = player.getClientConnection().getHddSerial();
		if (hddSerial == null || hddSerial.isEmpty()) {
			sendInfo(player, "无法锁定您的账号. 请重新登录后再试.");
			return;
		}
		// 获取当前玩家帐号状态
		boolean isLocked = hddSerial.equals(player.getAccount().getAllowedHddSerial());
		if (isLocked) {
			// 锁定时解锁
			player.getAccount().setAllowedHddSerial(null);
			LoginServer.getInstance().sendPacket(new SM_CHANGE_ALLOWED_HDD_SERIAL(player.getAccount()));
			sendInfo(player, "你已解锁当前计算机登录.");
		} else {
			// 未锁定时锁定
			player.getAccount().setAllowedHddSerial(player.getClientConnection().getHddSerial());
			LoginServer.getInstance().sendPacket(new SM_CHANGE_ALLOWED_HDD_SERIAL(player.getAccount()));
			sendInfo(player, "你已锁定当前计算机登录.");
		}
	}

	// 锁定结果 经验获取
	private void lockLevel(Player player) {
		// 查看当前等级是否已锁定
		boolean isLevelLocked = player.getCommonData().getNoExp();
		if (isLevelLocked) {
			// 已锁定时解锁
			player.getCommonData().setNoExp(false);
			sendInfo(player, "你已解锁当前等级, 现在可以获取经验.");
		} else {
			// 未锁定时锁定
			player.getCommonData().setNoExp(true);
			sendInfo(player, "你已锁定当前等级, 现在无法获取经验.");
		}
	}
}
