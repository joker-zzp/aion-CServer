package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Wakizashi
 */
public class AddExp extends AdminCommand {

	public AddExp() {
		super("addexp", "增加或减少玩家的经验值");

		setSyntaxInfo("<经验值> - 要添加的经验值 可以为负数");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		Player target = admin;

		if (admin.getTarget() instanceof Player)
			target = (Player) admin.getTarget();

		long exp;
		try {
			exp = Long.parseLong(params[0]);
		} catch (NumberFormatException e) {
			sendInfo(admin, "无效的经验值 必须是数字");
			return;
		}

		long resultExp = Math.max(0, target.getCommonData().getExp() + exp);
		target.getCommonData().setExp(resultExp);
		sendInfo(admin, "你为" + target.getName() + "添加了" + exp + "点经验值");
	}
}