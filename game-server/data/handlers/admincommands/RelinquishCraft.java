package admincommands;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.model.craft.Profession;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author synchro2, Neon
 */
public class RelinquishCraft extends AdminCommand {

  public RelinquishCraft() {
    super("relinquishcraft", "移除玩家的制作专家或大师状态。");

    // @formatter:off
    setSyntaxInfo(
      "<skillId> <expert|master> - 移除目标玩家指定制作技能的大师或专家状态。",
      "<name> <skillId> <expert|master> - 移除指定玩家指定制作技能的大师或专家状态。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length < 1) {
      sendInfo(admin);
      return;
    }

    if ("help".equalsIgnoreCase(params[0])) {
      sendInfo(admin);
      return;
    }

    if (params.length < 2) {
      sendInfo(admin);
      return;
    }

    int i = 0;
    Player target;
    if (params.length == 3) {
      String playerName = Util.convertName(params[i++]);
      target = World.getInstance().getPlayer(playerName);
      if (target == null) {
        PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(playerName));
        return;
      }
    } else {
      if (admin.getTarget() instanceof Player)
        target = (Player) admin.getTarget();
      else {
        PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
        return;
      }
    }

    Profession profession = Profession.getBySkillId(NumberUtils.toInt(params[i++]));
    if (profession == null || !profession.isCrafting()) {
      sendInfo(admin, "无效的技能ID。");
        return;
      }

      if ("expert".equalsIgnoreCase(params[i])) {
        if (RelinquishCraftStatus.relinquishExpertStatus(target, profession, 0))
          sendInfo(admin, "成功移除了" + profession + "的专家状态。");
        else
          sendInfo(admin, target.getName() + "没有" + profession + "的专家状态。");
      } else if ("master".equalsIgnoreCase(params[i])) {
        if (RelinquishCraftStatus.relinquishMasterStatus(target, profession, 0))
          sendInfo(admin, "成功移除了" + profession + "的大师状态。");
        else
          sendInfo(admin, target.getName() + "没有" + profession + "的大师状态。");
    } else
      sendInfo(admin);
  }
}