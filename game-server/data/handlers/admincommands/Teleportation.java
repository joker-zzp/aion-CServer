package admincommands;

import java.awt.Color;

import com.aionemu.gameserver.model.gameobjects.player.CustomPlayerState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 传送模式命令类 - 允许管理员启用或禁用鼠标点击移动时的传送模式
 * 
 * @author cura, Neon
 */
public class Teleportation extends AdminCommand {

	/**
	 * 创建传送模式命令
	 */
	public Teleportation() {
		super("teleportation", "切换鼠标点击移动时的传送模式。");
	}

	@Override
	public void execute(Player admin, String... params) {
		// 支持help参数
		if (params.length == 1 && "help".equalsIgnoreCase(params[0])) {
			sendInfo(admin, "//teleportation - 切换鼠标点击移动时的传送模式\n" +
				"当传送模式激活时，点击地图任意位置会直接传送到该位置。");
			return;
		}
		
		if (admin.isInCustomState(CustomPlayerState.TELEPORTATION_MODE))
			admin.unsetCustomState(CustomPlayerState.TELEPORTATION_MODE);
		else
			admin.setCustomState(CustomPlayerState.TELEPORTATION_MODE);
		sendInfo(admin, "传送模式现已" + (admin.isInCustomState(CustomPlayerState.TELEPORTATION_MODE)
			? ChatUtil.color("激活", Color.GREEN) : ChatUtil.color("关闭", Color.RED)) + "。");
	}
}