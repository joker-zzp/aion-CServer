package admincommands;

import java.util.Collections;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_STATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Divinity, Neon
 */
public class Invis extends AdminCommand {

	public Invis() {
		super("invis", "设置/取消高级隐身状态。");
	}

	@Override
	public void execute(Player player, String... params) {
		if (!player.isInVisualState(CreatureVisualState.HIDE20)) {
			player.getEffectController().setAbnormal(AbnormalState.HIDE);
			player.setVisualState(CreatureVisualState.HIDE20);
			player.getController().onHide();
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_EFFECT_INVISIBLE_BEGIN());
		} else {
			player.getEffectController().unsetAbnormal(AbnormalState.HIDE);
			player.unsetVisualState(CreatureVisualState.HIDE20);
			player.getController().onHideEnd();
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_EFFECT_INVISIBLE_END());
		}
		PacketSendUtility.broadcastPacket(player, new SM_PLAYER_STATE(player), true);
		// 这是必需的，因为如果没有技能，这个包不会自动发送(过时的异常状态可能会在开设私人商店等情况下导致问题)
		PacketSendUtility.sendPacket(player, new SM_ABNORMAL_STATE(Collections.emptyList(), player.getEffectController().getAbnormals(), 0));
	}
}