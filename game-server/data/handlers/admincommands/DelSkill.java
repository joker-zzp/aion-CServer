package admincommands;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author xTz
 */
public class DelSkill extends AdminCommand {

	public DelSkill() {
		super("delskill");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1 || params.length > 2) {
			PacketSendUtility.sendMessage(admin, "未检测到参数\n" + "请使用 //delskill <玩家名称> <all | 技能ID>\n" + "或者使用 //delskill [目标] <all | 技能ID>");
			return;
		}

		Player player;
		PlayerSkillList playerSkillList = null;
		String recipient = null;
		recipient = Util.convertName(params[0]);
		int skillId = 0;
		if (params.length == 2) {
			player = World.getInstance().getPlayer(recipient);
			if (player == null) {
				PacketSendUtility.sendMessage(admin, "指定的玩家不在线.");
				return;
			}

			if ("all".startsWith(params[1]))
				playerSkillList = player.getSkillList();
			else {
				try {
					skillId = Integer.parseInt(params[1]);
				} catch (NumberFormatException e) {
					PacketSendUtility.sendMessage(admin, "参数 1 必须是整数或 <all>.");
					return;
				}

				if (!check(admin, player, skillId))
					return;
			}
			apply(admin, player, skillId, playerSkillList);

		}
		if (params.length == 1) {
			VisibleObject target = admin.getTarget();
			if (target == null) {
				PacketSendUtility.sendMessage(admin, "您应该先选择一个目标!");
				return;
			}

			if (target instanceof Player) {
				player = (Player) target;

				if ("all".startsWith(params[0]))
					playerSkillList = player.getSkillList();
				else {
					try {
						skillId = Integer.parseInt(params[0]);
					} catch (NumberFormatException e) {
						PacketSendUtility.sendMessage(admin, "参数 0 必须是整数或 <all>.");
						return;
					}

					if (!check(admin, player, skillId))
						return;
				}
				if (target instanceof Player)
					apply(admin, player, skillId, playerSkillList);
			} else
				PacketSendUtility.sendMessage(admin, "此命令只能用于玩家!");
		}
	}

	private static boolean check(Player admin, Player player, int skillId) {
		if (skillId != 0 && !player.getSkillList().isSkillPresent(skillId)) {
			PacketSendUtility.sendMessage(admin, "玩家没有这个技能.");
			return false;
		}
		if (player.getSkillList().getSkillEntry(skillId).isStigmaSkill()) {
			PacketSendUtility.sendMessage(admin, "您不能删除圣痕技能.");
			return false;
		}
		return true;
	}

	public void apply(Player admin, Player player, int skillId, PlayerSkillList playerSkillList) {
		if (skillId != 0) {
			SkillLearnService.removeSkill(player, skillId);
			PacketSendUtility.sendMessage(admin, "您已成功删除指定技能.");
		} else {
			for (PlayerSkillEntry skillEntry : playerSkillList.getAllSkills()) {
				if (!skillEntry.isStigmaSkill()) {
					SkillLearnService.removeSkill(player, skillEntry.getSkillId());
				}
			}

			PacketSendUtility.sendMessage(admin, "您已成功删除所有技能.");
		}

	}

	@Override
	public void info(Player player, String message) {
		PacketSendUtility.sendMessage(player, "未检测到参数.\n" + "请使用 //delskill <玩家名称> <all | 技能ID>\n" + "或者使用 //delskill [目标] <all | 技能ID>");
	}
}