package admincommands;

import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Divinity, Neon
 * 命令用于让目标NPC说出指定的消息
 */
public class Say extends AdminCommand {

  public Say() {
    super("say", "让您的目标说出一条消息。");

    setSyntaxInfo("<消息内容> - 让您的目标说出消息（仅NPC有效）。");
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0 || (params.length > 0 && "help".equals(params[0]))) {
      sendInfo(admin);
      return;
    }

    if (!(admin.getTarget() instanceof Npc npc)) {
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
      return;
    }

    PacketSendUtility.broadcastPacket(admin, new SM_MESSAGE(npc, String.join(" ", params), ChatType.NORMAL), true);
  }
}