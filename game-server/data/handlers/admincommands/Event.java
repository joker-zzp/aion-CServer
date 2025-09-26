package admincommands;

import java.util.concurrent.atomic.AtomicInteger;

import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.CustomPlayerState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.TemporaryPlayerTeam;
import com.aionemu.gameserver.model.team.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * @author Nathan, Estrayl, Neon
 */
public class Event extends AdminCommand {

	public Event() {
		super("event", "管理活动功能和玩家活动状态.");

		// @formatter:off
		setSyntaxInfo(
			"<setStatus> [名称] - 禁用指定玩家的AP获取/损失并将其设置为活动状态.",
			"<setGroupStatus> [名称] - 获取并设置指定玩家的队伍为活动状态, 并禁用他们的AP获取/损失.",
			"<setEnemy> <cancel|team|ffa> [名称] - 设置特定状态(cancel: 正常, team: 玩家队伍外的所有人都是敌人, ffa: 所有人都是敌人).",
			"<pvpSpawn> [asmo|elyos] - 为指定种族设置复活点.",
			"<clearInstance> - 清空您创建的整个副本.",
			"<announce> <文本> - 向所有处于活动状态的玩家发送黄色消息.",
			"<list> - 列出所有处于活动状态的玩家.",
			"<removeAll> - 移除所有玩家的活动状态."
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		if (params[0].equalsIgnoreCase("pvpSpawn")) {
			if (params[1].equalsIgnoreCase("asmo")) {
				TeleportService.setEventPos(admin.getPosition(), Race.ASMODIANS);
				sendInfo(admin, "已设置魔族活动重生点!");
			} else if (params[1].equalsIgnoreCase("elyos") || params[1].equalsIgnoreCase("ely")) {
				TeleportService.setEventPos(admin.getPosition(), Race.ELYOS);
				sendInfo(admin, "已设置天族活动重生点!");
			} else {
				sendInfo(admin, "无效的种族参数!");
			}
		} else if (params[0].equalsIgnoreCase("clearInstance")) {
			clearInstance(admin);
		} else if (params[0].equalsIgnoreCase("announce")) {
			StringBuilder sb = new StringBuilder();
			sb.append(ChatUtil.name(admin)).append(':');
			for (int i = 1; i < params.length; i++)
				sb.append(" ").append(params[i]);

			World.getInstance().forEachPlayer(p -> {
				if (p.isInCustomState(CustomPlayerState.EVENT_MODE) || p == admin)
					PacketSendUtility.sendMessage(p, sb.toString(), ChatType.BRIGHT_YELLOW_CENTER);
			});
		} else if (params[0].equalsIgnoreCase("list")) {
			StringBuilder sb = new StringBuilder("处于活动状态的玩家:");
			World.getInstance().getAllPlayers().stream().filter(p -> p.isInCustomState(CustomPlayerState.EVENT_MODE))
				.forEach(p -> sb.append("\n\t").append(ChatUtil.name(p)));
			sendInfo(admin, sb.toString());
		} else if (params[0].equalsIgnoreCase("removeAll")) {
			for (Player player : World.getInstance().getAllPlayers())
				setEventState(admin, player, true);
		} else if (params[0].equalsIgnoreCase("setStatus")) {
			Player player = getPlayer(admin, params.length > 1 ? params[1] : null);
			if (player == null)
				return;
			setEventState(admin, player, false);
		} else if (params[0].equalsIgnoreCase("setGroupStatus")) {
			Player player = getPlayer(admin, params.length > 1 ? params[1] : null);
			if (player == null)
				return;
			TemporaryPlayerTeam<?> team = player.getCurrentTeam();
			if (team == null) {
				sendInfo(admin, "目标不在队伍或联盟中!");
				return;
			}
			for (Player p : team.getOnlineMembers())
				setEventState(admin, p, false);
		} else if (params.length > 1 && params[0].equalsIgnoreCase("setEnemy")) {
			Player player = getPlayer(admin, params.length > 2 ? params[2] : null);
			if (player == null)
				return;
			if (!player.isInCustomState(CustomPlayerState.EVENT_MODE)) {
				sendInfo(admin, player.getName() + " 未处于活动状态");
				return;
			}
			boolean ffaTeamMode = false;
			String msg = "不再处于自由对战状态.";
			if (params[1].equalsIgnoreCase("cancel")) {
				player.unsetCustomState(CustomPlayerState.ENEMY_OF_ALL_PLAYERS);
			} else if (params[1].equalsIgnoreCase("team")) {
				player.setCustomState(CustomPlayerState.ENEMY_OF_ALL_PLAYERS);
				msg = "现在处于队伍自由对战状态.";
				ffaTeamMode = true;
			} else if (params[1].equalsIgnoreCase("ffa")) {
				player.setCustomState(CustomPlayerState.ENEMY_OF_ALL_PLAYERS);
				msg = "现在处于自由对战状态.";
				PlayerGroupService.removePlayer(player);
				PlayerAllianceService.removePlayer(player);
			} else {
				sendInfo(admin);
				return;
			}
			player.setInFfaTeamMode(ffaTeamMode);
			player.getController().onChangedPlayerAttributes();
			sendInfo(admin, ChatUtil.name(player) + " " + msg);
			PacketSendUtility.sendMessage(player, "您" + msg, ChatType.BRIGHT_YELLOW_CENTER);
		} else {
			sendInfo(admin);
		}
	}

	private Player getPlayer(Player admin, String name) {
		Player player = null;
		if (name != null) {
			String playerName = Util.convertName(name);
			player = World.getInstance().getPlayer(playerName);
			if (player == null) {
				PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(playerName));
			}
		} else if (admin.getTarget() instanceof Player target) {
			player = target;
		} else {
			PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
		}
		return player;
	}

	private void clearInstance(Player admin) {
		WorldMapInstance map = admin.getPosition().getWorldMapInstance();
		if (!map.getParent().isInstanceType()) {
			sendInfo(admin, "此地图不是副本!");
			return;
		}
		if (map.getRegisteredCount() != 1 || !map.isRegistered(admin.getObjectId())) {
			sendInfo(admin, "此副本不是由您创建的, 您不能在此删除NPC. 使用//goto创建一个新的副本!");
			return;
		}
		AtomicInteger count = new AtomicInteger();
		map.forEachNpc(npc -> {
			npc.getController().delete();
			count.getAndIncrement();
		});
		map.forEachDoor(door -> door.setOpen(true));

		sendInfo(admin, "已删除 " + count + " 个NPC.");
	}

	private void setEventState(Player admin, Player player, boolean onlyRemove) {
		if (player.isInCustomState(CustomPlayerState.EVENT_MODE)) {
			player.unsetCustomState(CustomPlayerState.EVENT_MODE);
			player.unsetCustomState(CustomPlayerState.ENEMY_OF_ALL_PLAYERS);
			player.setInFfaTeamMode(false);
			player.getController().onChangedPlayerAttributes();
			sendInfo(admin, ChatUtil.name(player) + " 已被移出活动状态.");
			PacketSendUtility.sendMessage(player, "您已被移出活动状态!", ChatType.BRIGHT_YELLOW_CENTER);
		} else if (!onlyRemove) {
			player.setCustomState(CustomPlayerState.EVENT_MODE);
			sendInfo(admin, ChatUtil.name(player) + " 已设置为活动状态.");
			PacketSendUtility.sendMessage(player,
				"您现在处于活动状态. 请注意, 在未移除此状态的情况下, 您不允许离开活动区域!",
				ChatType.BRIGHT_YELLOW_CENTER);
		}
	}

}