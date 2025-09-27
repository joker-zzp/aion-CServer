package admincommands;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Estrayl
 */
public class Coords extends AdminCommand {

  public Coords() {
    super("coords", "显示目标的当前坐标.");
  }

  @Override
  public void execute(Player admin, String... params) {
    VisibleObject target = admin.getTarget() == null ? admin : admin.getTarget();
    sendInfo(admin, StringUtils.capitalize(target.getName()) + "的位置:\n" + target.getPosition().toCoordString());
  }
}