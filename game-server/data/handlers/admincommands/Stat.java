package admincommands;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.AbsoluteStatOwner;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunctionProxy;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 状态信息命令类 - 显示目标的状态信息
 * @author MrPoke
 */
public class Stat extends AdminCommand {

	public Stat() {
		super("stat", "显示目标的状态信息。");

		// @formatter:off
		setSyntaxInfo(
			"<stat> [details] - 显示指定状态的活动状态函数（默认：仅显示名称，可选：详细信息）。",
			"<abs> <状态集ID|cancel> - 应用absolute_stats.xml中指定状态集ID的固定属性或取消它们。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0 || "help".equals(params[0])) {
			sendInfo(admin);
			return;
		}

		VisibleObject target = admin.getTarget();
		if (!(target instanceof Creature)) {
			PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
			return;
		}
		Creature creature = (Creature) target;

		if (params.length == 1) {
			List<IStatFunction> stats = creature.getGameStats().getStatsSorted(StatEnum.valueOf(params[0]));
			for (IStatFunction stat : stats) {
				sendInfo(admin, stat.toString());
			}
		} else if (params.length == 2 && "details".equals(params[1])) {
			List<IStatFunction> stats = creature.getGameStats().getStatsSorted(StatEnum.valueOf(params[0]));
			for (IStatFunction stat : stats) {
				String details = collectDetails(stat);
				sendInfo(admin, details);
			}
		} else if ("abs".equals(params[0])) {
			if (!(target instanceof Player)) {
				sendInfo(admin, "只能选择玩家作为目标");
				return;
			}
			AbsoluteStatOwner absStats = ((Player) target).getAbsoluteStats();
			try {
				Integer templateId = Integer.parseInt(params[1]);
				absStats.setTemplate(templateId);
				absStats.apply();
				if (absStats.isActive()) {
					sendInfo(admin, "成功应用固定属性");
				} else {
					sendInfo(admin, "该模板不存在！");
				}
			} catch (NumberFormatException ex) {
				if (!"cancel".equalsIgnoreCase(params[1])) {
					sendInfo(admin, "不是有效数字");
					return;
				}
				if (!absStats.isActive()) {
					sendInfo(admin, "没有需要取消的设置");
					return;
				}
				absStats.cancel();
				sendInfo(admin, "成功取消固定属性");
				PacketSendUtility.sendPacket((Player) target, new SM_STATS_INFO((Player) target));
			}
		} else {
			sendInfo(admin);
		}
	}

	private String collectDetails(IStatFunction stat) {
		StringBuilder sb = new StringBuilder();
		sb.append(stat.toString() + "\n");
		if (stat instanceof StatFunctionProxy) {
			StatFunctionProxy proxy = (StatFunctionProxy) stat;
			sb.append(" -- " + proxy.getProxiedFunction().toString());
		}
		StatOwner owner = stat.getOwner();
		if (owner instanceof Effect) {
			Effect effect = (Effect) owner;
			sb.append("\n -- skillId: " + effect.getSkillId());
			sb.append("\n -- skillName: " + effect.getSkillName());
		}
		return sb.toString();
	}

}