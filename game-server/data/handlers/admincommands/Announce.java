package admincommands;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Neon
 */
public class Announce extends AdminCommand {

  public Announce() {
    super("announce", "发送服务器范围内的通知");

    setSyntaxInfo(
      "<n|a> <消息> - 以<n>名称或<a>匿名方式发送消息",
      "<ely|asmo> <消息> - 向<ely>天族或<asmo>魔族玩家发送匿名消息"
    );
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length <= 1) {
      sendInfo(admin);
      return;
    }

    String flag = params[0].toLowerCase();
    String[] flags = { "n", "a", "ely", "asmo" };
    if (!ArrayUtils.contains(flags, flag)) {
      sendInfo(admin);
      return;
    }

    StringBuilder sb = new StringBuilder();
    Race allowedRace = null;
    switch (flag) {
      case "n":
        sb.append(ChatUtil.name(admin) + ":");
        break;
      case "a":
        sb.append("公告:");
        break;
      case "ely":
        sb.append("天族:");
        allowedRace = Race.ELYOS;
        break;
      case "asmo":
        sb.append("魔族:");
        allowedRace = Race.ASMODIANS;
        break;
    }

    for (int i = 1; i < params.length; i++)
      sb.append(" ").append(params[i]);

    for (Player player : World.getInstance().getAllPlayers())
      if (allowedRace == null || player.getRace() == allowedRace || validateAccess(player))
        PacketSendUtility.sendMessage(player, sb.toString(), ChatType.BRIGHT_YELLOW_CENTER);
  }
}