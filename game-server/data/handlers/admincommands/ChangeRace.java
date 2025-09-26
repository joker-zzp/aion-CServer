package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author ginho1
 */
public class ChangeRace extends AdminCommand {

	public ChangeRace() {
		super("changerace", "将您的种族切换到对立阵营。");
	}

	@Override
	public void execute(Player admin, String... params) {
		admin.getCommonData().setRace(admin.getOppositeRace());
		admin.getController().onChangedPlayerAttributes();
		admin.getController().updateNearbyQuests();
	}
}