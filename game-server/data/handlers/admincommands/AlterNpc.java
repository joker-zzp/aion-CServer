package admincommands;

import java.util.Arrays;
import java.util.EnumSet;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.functions.StatSetFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Estrayl
 */
public class AlterNpc extends AdminCommand {

	private static final EnumSet<StatEnum> allowedStats = EnumSet.of(StatEnum.MAXHP, StatEnum.PHYSICAL_ATTACK, StatEnum.MAGICAL_ATTACK,
		StatEnum.PHYSICAL_DEFENSE, StatEnum.MAGICAL_DEFEND, StatEnum.MAGICAL_RESIST, StatEnum.PARRY, StatEnum.PHYSICAL_ACCURACY,
		StatEnum.MAGICAL_ACCURACY, StatEnum.PHYSICAL_CRITICAL_RESIST, StatEnum.MAGIC_SKILL_BOOST_RESIST);

	public AlterNpc() {
		super("alternpc", "用于修改NPC的属性");

		// @formatter:off
		setSyntaxInfo(
			"<list> - 显示所有可修改的属性",
			"<change> [属性] [数值] - 将相应的属性更改为指定数值"
		);
		// @formatter:on
	}

	@Override
	protected void execute(Player player, String... params) {
		if (params.length < 1) {
			sendInfo(player);
			return;
		}
		if (params[0].equalsIgnoreCase("list"))
			showList(player);
		else if (params[0].equalsIgnoreCase("change") && params.length > 2)
			changeStat(player, params);
		else
			sendInfo(player);
	}

	private void changeStat(Player player, String[] params) {
		StatEnum toModify = StatEnum.valueOf(params[1].toUpperCase());
		if (!allowedStats.contains(toModify)) {
			sendInfo(player, "'" + params[0] + "' 不被支持");
			return;
		}

		if (!(player.getTarget() instanceof Npc)) {
			sendInfo(player, "你应该先选择一个NPC");
			return;
		}
		Npc target = (Npc) player.getTarget();
		int newValue = parseValue(params[2]);
		if (newValue < 1) {
			sendInfo(player, "新的属性值必须是数字");
			return;
		}

		target.getGameStats().addEffect(null, Arrays.asList(new StatSetFunction(toModify, newValue)));
		if (toModify == StatEnum.MAXHP)
			target.getLifeStats().setCurrentHp(newValue);
		PacketSendUtility.sendMessage(player, "已将 " + target + " 的 " + toModify.toString() + " 修改为 " + params[2] + "。");
	}

	private void showList(Player player) {
		String msg = "";
		for (StatEnum se : allowedStats)
			msg += "\n" + se.toString();
		PacketSendUtility.sendMessage(player, "当前允许修改的属性: " + msg + "。");
	}

	private int parseValue(String value) {
		try {
			return Integer.parseInt(value.replace("_", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}