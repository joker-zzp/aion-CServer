package admincommands;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author KID, nrg
 */
public class BanMac extends AdminCommand {

  public BanMac() {
    super("banmac");
  }

  @Override
  public void execute(Player player, String... params) {
    if (params == null || params.length < 1) {
      info(player, "请添加一个或多个参数");
      return;
    }

    int time;
    String address;
    String targetName = "direct_type";

    // try parsing
    try {
      time = Integer.parseInt(params[0]);

      if (time == 0) // 0 is 10 years since system don't allow infinte banns without rework - it's pseudo infinity
        time = 60 * 24 * 365 * 10;
    } catch (NumberFormatException e) {
        info(player, "请输入有效的分钟数");
        return;
      }

    // is mac defined?
    if (params.length > 1) {
      address = params[1];
    } else { // no address defined
      VisibleObject target = player.getTarget();
      if (target instanceof Player) {
        if (target.equals(player)) {
            info(player, "请取消选择自己。");
            return;
          }

        Player targetpl = (Player) target;
        address = targetpl.getClientConnection().getMacAddress();
        targetName = targetpl.getName();
        targetpl.getClientConnection().close();
      } else {
          info(player, "您应该选择一个玩家或提供MAC地址");
          return;
        }
    }

    BannedMacManager.getInstance().banAddress(address, System.currentTimeMillis() + time * 60 * 1000,
      "author=" + player.getName() + ", " + player.getObjectId() + "; target=" + targetName);
  }

  @Override
  public void info(Player player, String message) {
    if (!message.equals(""))
      PacketSendUtility.sendMessage(player, message);
    PacketSendUtility.sendMessage(player, "用法: //banmac [分钟数] <mac>");
    PacketSendUtility.sendMessage(player, "注意: 0分钟表示永久封禁");
  }

}