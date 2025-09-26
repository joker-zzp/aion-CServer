package admincommands;

import com.aionemu.gameserver.custom.instance.CustomInstanceRankEnum;
import com.aionemu.gameserver.custom.instance.CustomInstanceService;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Estrayl
 */
public class CustomInstance extends AdminCommand {

	public CustomInstance() {
		super("cinstance", "自定义副本管理命令工具");

		// @formatter:off
		setSyntaxInfo(
			"<removecd> - 移除选定玩家的自定义副本冷却时间",
			"<getrank> - 获取选定玩家的当前自定义副本排名",
			"<setrank> [newRank] - 将选定玩家的自定义副本排名更改为给定值"
		);
		// @formatter:on
	}

	@Override
	protected void execute(Player player, String... params) {
		if (params.length < 1) {
			sendInfo(player);
			return;
		}

		switch (params[0].toLowerCase()) {
			case "removecd":
				if (player.getTarget() instanceof Player targetPlayer) {
					if (CustomInstanceService.getInstance().resetEntryCooldown(targetPlayer.getObjectId())) {
						PacketSendUtility.sendMessage(player, "已成功移除玩家 " + targetPlayer.getName() + " 的自定义副本冷却时间");
					} else {
						PacketSendUtility.sendMessage(player, "玩家 " + targetPlayer.getName() + " 不需要重置冷却时间");
					}
				} else {
					PacketSendUtility.sendMessage(player, "请先选择一个玩家目标");
				}
				break;
			case "getrank":
				if (player.getTarget() instanceof Player targetPlayer) {
					int rank = CustomInstanceService.getInstance().loadOrCreateRank(targetPlayer.getObjectId()).getRank();
					PacketSendUtility.sendMessage(player,
						targetPlayer.getName() + " 的当前排名是 " + CustomInstanceRankEnum.getRankDescription(rank) + "(" + rank + ")");
				} else {
					PacketSendUtility.sendMessage(player, "请先选择一个玩家目标");
				}
				break;
			case "setrank":
				if (params.length < 2) {
					sendInfo(player);
					return;
				}
				setNewRank(player, params[1]);
				break;
		}
	}

	private void setNewRank(Player player, String newRank) {
		int rank;
		try {
			rank = Integer.parseInt(newRank);
		} catch (NumberFormatException e) {
			sendInfo(player, "新排名必须是一个数字");
			return;
		}
		VisibleObject target = player.getTarget();
		if (player.getTarget() instanceof Player targetPlayer) {
			CustomInstanceService.getInstance().changePlayerRank(targetPlayer.getObjectId(), rank, 0);
			PacketSendUtility.sendMessage(player,
				"已将 " + target.getName() + " 的排名更改为 " + rank + ", 对应描述为 " + CustomInstanceRankEnum.getRankDescription(rank));
		} else {
			sendInfo(player, "请先选择一个玩家目标");
		}

	}
}