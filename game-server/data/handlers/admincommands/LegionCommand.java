package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author KID
 */
public class LegionCommand extends AdminCommand {

	public LegionCommand() {
		super("legion", "修改军团信息。");

		// @formatter:off
		setSyntaxInfo(
			"info <军团名称> - 列出军团成员。",
			"add <军团名称> <玩家名称> - 将玩家添加到军团。",
			"kick <玩家名称> - 将玩家从其所在军团中踢出。",
			"disband <军团名称> - 解散军团。",
			"rename <军团名称> <新名称> - 更改军团名称。",
			"setbg <军团名称> <玩家名称> - 更改军团的军团长。",
			"setlevel <军团名称> <等级> - 更改军团等级。",
			"setpoints <军团名称> <点数> - 更改军团的贡献点。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length < 2) {
			sendInfo(player);
			return;
		}

		if (params[0].equalsIgnoreCase("disband")) {
			Legion legion = getLegion(params[1]);
			LegionService.getInstance().disbandLegion(legion);
			sendInfo(player, "军团 " + legion.getName() + " 已被解散。");
		} else if (params[0].equalsIgnoreCase("setlevel") && params.length >= 3) {
			Legion legion = getLegion(params[1]);
			int level = Integer.parseInt(params[2]);
			if (level < 1 || level > 8) {
				sendInfo(player, "军团等级必须在1到8之间。");
				return;
			} else if (level == legion.getLegionLevel()) {
				sendInfo(player, "军团 " + params[1] + " 已经处于等级 " + level);
				return;
			}
			int old = legion.getLegionLevel();
			LegionService.getInstance().changeLevel(legion, level, true);
				sendInfo(player, "军团 " + legion.getName() + " 的等级已从 " + old + " 更改为 " + level);
		} else if (params[0].equalsIgnoreCase("setpoints") && params.length >= 3) {
			Legion legion = getLegion(params[1]);
			long points = Long.parseLong(params[2]);
			if (points < 1) {
				sendInfo(player, "贡献点必须大于零。");
				return;
			}
			long old = legion.getContributionPoints();
			LegionService.getInstance().setContributionPoints(legion, points, true);
				sendInfo(player, "军团 " + legion.getName() + " 的贡献点已从 " + old + " 更改为 " + points);
		} else if (params[0].equalsIgnoreCase("rename") && params.length >= 3) {
			Legion legion = getLegion(params[1]);
			String old = legion.getName();
			if (LegionService.getInstance().tryRename(legion, params[2], player, null))
					sendInfo(player, "军团 " + old + " 已重命名为 " + legion.getName() + "。");
		} else if (params[0].equalsIgnoreCase("info")) {
			Legion legion = getLegion(params[1]);
			sendInfo(player, "军团名称: " + legion.getName());
			sendInfo(player, "等级: " + legion.getLegionLevel());
			sendInfo(player, "贡献点: " + legion.getContributionPoints());
			sendInfo(player, "成员 (" + legion.getLegionMembers().size() + "):");
			for (int memberId : legion.getLegionMembers()) {
				PlayerCommonData pcd = PlayerService.getOrLoadPlayerCommonData(memberId);
				String brigadeGeneralInfo = memberId == legion.getBrigadeGeneral() ? ", 军团长" : "";
				sendInfo(player, "\t" + pcd.getName() + " (等级 " + pcd.getLevel() + " " + pcd.getPlayerClass() + brigadeGeneralInfo + ")");
			}
		} else if (params[0].equalsIgnoreCase("kick")) {
			Player target = World.getInstance().getPlayer(Util.convertName(params[1]));
			if (target == null)
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(params[1]));
			else if (target.getLegionMember().getRank() == LegionRank.BRIGADE_GENERAL)
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GUILD_BANISH_CAN_BANISH_MASTER());
			else if (LegionService.getInstance().leaveLegion(target, true)) 
						sendInfo(player, target.getName() + " 已被踢出军团。");
					else
						sendInfo(player, target.getName() + " 无法被踢出军团。");
		} else if (params[0].equalsIgnoreCase("add") && params.length >= 3) {
			Legion legion = getLegion(params[1]);
			Player target = World.getInstance().getPlayer(Util.convertName(params[2]));
			if (target == null)
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(params[2]));
			else if (target.isLegionMember())
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GUILD_INVITE_HE_IS_OTHER_GUILD_MEMBER(target.getName()));
			else if (LegionService.getInstance().addToLegion(legion, target, player))
						sendInfo(player, target.getName() + " 已添加到 " + legion.getName() + "。");
		} else if (params[0].equalsIgnoreCase("setbg") && params.length >= 3) {
			Legion legion = getLegion(params[1]);
			Player target = World.getInstance().getPlayer(Util.convertName(params[2]));
			if (target == null) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(params[2]));
				return;
			}
			if (!legion.isMember(target.getObjectId())) {
						sendInfo(player, target.getName() + " 不是 " + legion.getName() + " 的成员。");
						return;
					}
					LegionService.getInstance().appointBrigadeGeneral(target);
					sendInfo(player, "军团长已更改为 " + target.getName() + "。");
		} else {
			sendInfo(player);
		}
	}

	private Legion getLegion(String name) {
		if (name.contains("_"))
			name = name.replaceAll("_", " ");
		Legion legion = LegionService.getInstance().getLegion(name.toLowerCase());
		if (legion == null) {
				throw new IllegalArgumentException("军团 " + name + " 不存在。");
			}
		return legion;
	}
}