package admincommands;

import static com.aionemu.gameserver.configs.main.AutoGroupConfig.*;

import com.aionemu.gameserver.model.autogroup.AutoGroupType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.PeriodicInstanceManager;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author ViAl, Estrayl
 */
public class Instance extends AdminCommand {

	public Instance() {
		super("instance", "激活或停用PvP副本的注册。");

		// @formatter:off
		setSyntaxInfo(
				"<open|close> dredgion - 开启/关闭德拉基翁(6vs6)的注册",
				"<open|close> id - 开启/关闭伊德盖尔神殿(6vs6)的注册",
				"<open|close> eob - 开启/关闭被吞噬的蛇神殿桥(6vs6)的注册",
				"<open|close> kb - 开启/关闭卡玛尔战场(12vs12)的注册",
				"<open|close> iww - 开启/关闭铁墙前线(24vs24)的注册"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length < 2) {
			sendInfo(player);
			return;
		}
		if (params[0].equalsIgnoreCase("open"))
			openRegistration(player, params[1]);
		else if (params[0].equalsIgnoreCase("close"))
			closeRegistration(player, params[1]);
	}

	private void openRegistration(Player admin, String instanceName) {
		SM_SYSTEM_MESSAGE openingMsg;
		int maskId;
		long registrationPeriod;

		switch (instanceName.toLowerCase()) {
			case "dredgion" -> { // 仅开启泰拉特德拉基翁
				openingMsg = SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDDREADGION_03();
				maskId = 3;
				registrationPeriod = DREDGION_REGISTRATION_PERIOD;
			}
			case "eob" -> {
				openingMsg = SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDLDF5_Under_01_War();
				maskId = 108;
				registrationPeriod = ENGULFED_OPHIDAN_BRIDGE_REGISTRATION_PERIOD;
			}
			case "id" -> {
				openingMsg = SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDLDF5_Fortress_Re();
				maskId = 111;
				registrationPeriod = IDGEL_DOME_REGISTRATION_PERIOD;
			}
			case "iww" -> {
				openingMsg = SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDF5_TD_war();
				maskId = 109;
				registrationPeriod = IRON_WALL_WARFRONT_REGISTRATION_PERIOD;
			}
			case "kb" -> {
				openingMsg = SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_OPEN_IDKamar();
				maskId = 107;
				registrationPeriod = KAMAR_BATTLEFIELD_REGISTRATION_PERIOD;
			}
			default -> {
				openingMsg = null;
				maskId = 0;
				registrationPeriod = 0;
			}
		}
		if (maskId != 0) {
			if (PeriodicInstanceManager.getInstance().openRegistration(openingMsg, maskId, registrationPeriod))
				sendInfo(admin, AutoGroupType.getAGTByMaskId(maskId) + " 的注册现已开启。");
			else
				sendInfo(admin, AutoGroupType.getAGTByMaskId(maskId) + " 的注册已经开启。");
		} else {
			sendInfo(admin, "未找到名为 " + instanceName + " 的副本。");
		}
	}

	private void closeRegistration(Player admin, String instanceName) {
		int maskId;

		switch (instanceName.toLowerCase()) {
			case "dredgion" -> maskId = 3;
			case "eob" -> maskId = 108;
			case "id" -> maskId = 111;
			case "iww" -> maskId = 109;
			case "kb" -> maskId = 107;
			default -> maskId = 0;
		}

		if (maskId != 0) {
			if (PeriodicInstanceManager.getInstance().closeRegistration(maskId))
				sendInfo(admin, AutoGroupType.getAGTByMaskId(maskId) + " 的注册现已关闭。");
			else
				sendInfo(admin, AutoGroupType.getAGTByMaskId(maskId) + " 的注册未开启。");
		} else {
			sendInfo(admin, "未找到名为 " + instanceName + " 的副本。");
		}
	}

}