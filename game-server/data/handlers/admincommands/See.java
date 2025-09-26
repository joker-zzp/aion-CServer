package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureSeeState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Mathew
 */
public class See extends AdminCommand {

	public See() {
		super("see", "让你可以看到隐藏的NPC和玩家。");
		setSyntaxInfo("显示/隐藏帮助信息或切换可见状态");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length > 0 && params[0].equalsIgnoreCase("help")) {
			sendInfo(admin);
			return;
		}
		
		if (admin.getSeeState() < 2) {
			admin.setSeeState(CreatureSeeState.SEARCH20);
			sendInfo(admin, ChatUtil.l10n(288645)); // 可以看到处于高级隐藏状态的目标。
		} else {
			admin.unsetSeeState(CreatureSeeState.SEARCH20);
			sendInfo(admin, "你失去了视野。");
		}
		PacketSendUtility.broadcastPacket(admin, new SM_PLAYER_STATE(admin), true);
		admin.updateKnownlist();
	}
}