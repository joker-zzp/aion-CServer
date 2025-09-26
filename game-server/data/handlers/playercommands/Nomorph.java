package playercommands;

import java.awt.*;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;


public class Nomorph extends PlayerCommand {

	public Nomorph() {
		super("nomorph", "Enables/disables 你当前的变形外观.");
		// help
		setSyntaxInfo(
			"[on/off] - 启用/关闭 变形对角色外观影响."
		);
	}


	@Override
	protected void execute(Player player, String... params) {
		boolean isNomorph = player.getTransformModel().getEventModelId() == player.getObjectTemplate().getTemplateId();
		if (isNomorph) {
			player.getTransformModel().setEventModelId(0);
		} else {
			player.getTransformModel().setEventModelId(player.getObjectTemplate().getTemplateId());
		}
		// 刷新外观
		player.getTransformModel().updateVisually();

		sendInfo(player, "变形外观已切换为 " + (isNomorph ? ChatUtil.color("关闭", Color.RED) : ChatUtil.color("开启", Color.GREEN)) + ".");
	}
}
