package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.player.SecurityTokenService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Artur
 * 用于生成和查看玩家安全令牌的命令
 */
public class SecurityToken extends AdminCommand {

	public SecurityToken() {
		super("stoken");
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length < 1 || "help".equals(params[0])) {
			sendInfo(player);
			return;
		}
		Player receiver = null;

		if (params[0].equals("show")) {
			receiver = World.getInstance().getPlayer(Util.convertName(params[1]));
			if (receiver == null) {
				PacketSendUtility.sendMessage(player, "找不到该玩家，可能他不在线");
				return;
			}

			if (!"".equals(receiver.getAccount().getSecurityToken())) {
				PacketSendUtility.sendMessage(player, "该玩家的安全令牌是: " + receiver.getAccount().getSecurityToken());
			} else {
				PacketSendUtility.sendMessage(player, "该玩家没有安全令牌！");
			}

		} else {
			receiver = World.getInstance().getPlayer(Util.convertName(params[0]));

			if (receiver == null) {
				PacketSendUtility.sendMessage(player, "找不到该玩家，可能他不在线");
				return;
			}

			SecurityTokenService.generateToken(receiver.getAccount());
			PacketSendUtility.sendMessage(player, "已为玩家 " + receiver.getName() + " 生成新的安全令牌");
		}

	}

	@Override
	public void info(Player admin, String message) {
		PacketSendUtility.sendMessage(admin, "语法: //stoken <玩家名> || //stoken show <玩家名>");
		PacketSendUtility.sendMessage(admin, "//stoken <玩家名> - 为指定玩家生成新的安全令牌");
		PacketSendUtility.sendMessage(admin, "//stoken show <玩家名> - 显示指定玩家的安全令牌");
	}

}