package admincommands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.DispelSlotType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Mrakobes, Loxo
 */
public class Heal extends AdminCommand {

	public Heal() {
		super("heal", "恢复HP、MP、DP、飞行时间和灵息能量。");

		// @formatter:off
		setSyntaxInfo(
			" - 恢复目标的HP、MP并移除灵魂疾病。",
			"<dp> - 恢复目标的DP。",
			"<fp> - 恢复目标的飞行时间。",
			"<repose> - 恢复目标的灵息能量。",
			"<number> - 按给定数量恢复目标的HP。",
			"<number%> - 按给定百分比恢复目标的HP。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {
		VisibleObject target = player.getTarget();
		if (!(target instanceof Creature)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
			return;
		}

		Creature creature = (Creature) target;

		if (params.length == 0) {
			creature.getLifeStats().increaseHp(TYPE.HP, creature.getLifeStats().getMaxHp());
			creature.getLifeStats().increaseMp(TYPE.HEAL_MP, creature.getLifeStats().getMaxMp(), 0, LOG.MPHEAL);
			creature.getEffectController().removeByDispelSlotType(DispelSlotType.SPECIAL2);
			if (!player.equals(creature))
				sendInfo(player, creature.getName() + " 已恢复。");
		} else if (params[0].equalsIgnoreCase("dp") && creature instanceof Player) {
			Player targetPlayer = (Player) creature;
			targetPlayer.getCommonData().setDp(targetPlayer.getGameStats().getMaxDp().getCurrent());
			if (!player.equals(creature))
				sendInfo(player, targetPlayer.getName() + " 的DP已完全恢复。");
		} else if (params[0].equalsIgnoreCase("fp") && creature instanceof Player) {
			Player targetPlayer = (Player) creature;
			targetPlayer.getLifeStats().setCurrentFp(targetPlayer.getLifeStats().getMaxFp());
			if (!player.equals(creature))
				sendInfo(player, targetPlayer.getName() + " 的飞行时间已完全恢复。");
		} else if (params[0].equalsIgnoreCase("repose") && creature instanceof Player) {
			Player targetPlayer = (Player) creature;
			PlayerCommonData pcd = targetPlayer.getCommonData();
			pcd.setCurrentReposeEnergy(pcd.getMaxReposeEnergy());
			PacketSendUtility.sendPacket(targetPlayer,
				new SM_STATUPDATE_EXP(pcd.getExpShown(), pcd.getExpRecoverable(), pcd.getExpNeed(), pcd.getCurrentReposeEnergy(), pcd.getMaxReposeEnergy()));
			if (!player.equals(creature))
				sendInfo(player, targetPlayer.getName() + " 的灵息能量已完全恢复。");
		} else {
			try {
				Matcher result = Pattern.compile("(.+)%").matcher(params[0]);
				int value;

				if (result.find()) {
					int hpPercent = Integer.parseInt(result.group(1));

					if (hpPercent < 100)
						value = (int) (hpPercent / 100f * creature.getLifeStats().getMaxHp());
					else
						value = creature.getLifeStats().getMaxHp();
				} else
					value = Integer.parseInt(params[0]);
				creature.getLifeStats().increaseHp(TYPE.HP, value);
				if (!player.equals(creature))
					sendInfo(player, creature.getName() + " 已恢复 " + value + " 点生命值！");
			} catch (Exception ex) {
				sendInfo(player);
			}
		}
	}
}