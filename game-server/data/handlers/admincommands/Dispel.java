package admincommands;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Hilgert, Estrayl
 */
public class Dispel extends AdminCommand {

	public Dispel() {
		super("dispel", "移除所有效果, 包括变形效果.");
	}

	@Override
	public void execute(Player admin, String... params) {
		VisibleObject target = admin.getTarget();
		if (target == null)
			target = admin;

		if (target instanceof Creature creature) {
			creature.getEffectController().removeAllEffects();
			creature.getEffectController().removeTransformEffects();
			sendInfo(admin, "已移除 " + target + " 的所有效果.");
		}
	}
}