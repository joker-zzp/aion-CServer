package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.CustomPlayerState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Neon
 */
public class Enemy extends AdminCommand {

	public Enemy() {
		super("enemy", "修改你对其他单位的敌意状态。");

		// @formatter:off
		setSyntaxInfo(
			"<all> [players|npcs] - 设置你的敌意状态(默认: 你是所有人的敌人, 可选: 你是所有玩家或所有NPC的敌人).",
			"<none> [players|npcs] - 取消你的敌意状态(默认: 你不是任何人的敌人, 可选: 你不是任何玩家或任何NPC的敌人).",
			"<cancel> - 将你的敌意状态重置为默认值."
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			sendInfo(player);
			return;
		}

		if (params[0].equalsIgnoreCase("all")) {
			if (params.length == 1) {
				player.unsetCustomState(CustomPlayerState.NEUTRAL_TO_EVERYONE);
				player.setCustomState(CustomPlayerState.ENEMY_OF_EVERYONE);
				sendInfo(player, "你现在是所有人的敌人.");
			} else if (params[1].equalsIgnoreCase("npcs")) {
				player.unsetCustomState(CustomPlayerState.ENEMY_OF_EVERYONE);
				player.unsetCustomState(CustomPlayerState.NEUTRAL_TO_ALL_NPCS);
				player.setCustomState(CustomPlayerState.ENEMY_OF_ALL_NPCS);
				sendInfo(player, "你现在是所有NPC的敌人.");
			} else if (params[1].equalsIgnoreCase("players")) {
				player.unsetCustomState(CustomPlayerState.ENEMY_OF_EVERYONE);
				player.unsetCustomState(CustomPlayerState.NEUTRAL_TO_ALL_PLAYERS);
				player.setCustomState(CustomPlayerState.ENEMY_OF_ALL_PLAYERS);
				sendInfo(player, "你现在是所有玩家的敌人.");
			} else {
				sendInfo(player);
				return;
			}
		} else if (params[0].equalsIgnoreCase("none")) {
			if (params.length == 1) {
				player.unsetCustomState(CustomPlayerState.ENEMY_OF_EVERYONE);
				player.setCustomState(CustomPlayerState.NEUTRAL_TO_EVERYONE);
				sendInfo(player, "你现在对所有人保持中立.");
			} else if (params[1].equalsIgnoreCase("npcs")) {
				player.unsetCustomState(CustomPlayerState.NEUTRAL_TO_EVERYONE);
				player.unsetCustomState(CustomPlayerState.ENEMY_OF_ALL_NPCS);
				player.setCustomState(CustomPlayerState.NEUTRAL_TO_ALL_NPCS);
				sendInfo(player, "你现在对所有NPC保持中立.");
			} else if (params[1].equalsIgnoreCase("players")) {
				player.unsetCustomState(CustomPlayerState.NEUTRAL_TO_EVERYONE);
				player.unsetCustomState(CustomPlayerState.ENEMY_OF_ALL_PLAYERS);
				player.setCustomState(CustomPlayerState.NEUTRAL_TO_ALL_PLAYERS);
				sendInfo(player, "你现在对所有玩家保持中立.");
			} else {
				sendInfo(player);
				return;
			}
		} else if (params[0].equalsIgnoreCase("cancel")) {
			player.unsetCustomState(CustomPlayerState.ENEMY_OF_EVERYONE);
			player.unsetCustomState(CustomPlayerState.NEUTRAL_TO_EVERYONE);
			sendInfo(player, "你现在对所有人来说恢复正常状态.");
		} else {
			sendInfo(player);
			return;
		}
		player.getController().onChangedPlayerAttributes();
	}

}