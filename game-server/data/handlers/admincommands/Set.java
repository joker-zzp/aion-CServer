package admincommands;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_INFO;
import com.aionemu.gameserver.services.ClassChangeService;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.abyss.GloryPointsService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 设置玩家属性命令
 * @author Nemiroff, ATracer, IceReaper, Sarynth, Artur
 */
public class Set extends AdminCommand {

  public Set() {
    super("set");
  }

  @Override
  public void execute(Player admin, String... params) {
    Player target = null;
    VisibleObject creature = admin.getTarget();

    if (admin.getTarget() instanceof Player) {
      target = (Player) creature;
    }

    if (params[0].equals("help")) {
      info(admin, "");
      return;
    }

    if (target == null) {
      PacketSendUtility.sendMessage(admin, "你需要先选择一个目标！");
      return;
    }

    if (params.length < 2) {
      PacketSendUtility.sendMessage(admin, "你需要输入第二个参数！");
      return;
    }
    String paramValue = params[1];

    if (params[0].equals("class")) {
      byte newClass;
      try {
        newClass = Byte.parseByte(paramValue);
      } catch (NumberFormatException e) {
        PacketSendUtility.sendMessage(admin, "你需要输入有效的第二个参数！");
        return;
      }

      ClassChangeService.setClass(target, PlayerClass.getPlayerClassById(newClass), true, true);
    } else if (params[0].equals("exp")) {
      long exp;
      try {
        exp = Long.parseLong(paramValue);
      } catch (NumberFormatException e) {
        PacketSendUtility.sendMessage(admin, "你需要输入有效的第二个参数！");
        return;
      }

      target.getCommonData().setExp(exp);
      PacketSendUtility.sendMessage(admin, "已将目标的经验值设置为 " + target.getCommonData().getExp());
    } else if (params[0].equals("ap")) {
      int ap;
      try {
        ap = Integer.parseInt(paramValue);
      } catch (NumberFormatException e) {
        PacketSendUtility.sendMessage(admin, "你需要输入有效的第二个参数！");
        return;
      }

      AbyssPointsService.setAp(target, ap);
      if (target == admin) {
        PacketSendUtility.sendMessage(admin, "已将你的深渊点数设置为 " + ap + "。");
      } else {
        PacketSendUtility.sendMessage(admin, "已将 " + target.getName() + " 的深渊点数设置为 " + ap + "。");
        PacketSendUtility.sendMessage(target, "管理员将你的深渊点数设置为 " + ap + "。");
      }
    } else if (params[0].equals("gp")) {
      int gp;
      try {
        gp = Integer.parseInt(paramValue);
      } catch (NumberFormatException e) {
        PacketSendUtility.sendMessage(admin, "你需要输入有效的第二个参数！");
        return;
      }
      GloryPointsService.modifyGpBy(target.getObjectId(), gp, false, false);
      if (target == admin) {
        PacketSendUtility.sendMessage(admin, "已将你的荣耀点数设置为 " + gp + "。");
      } else {
        PacketSendUtility.sendMessage(admin, "已将 " + target.getName() + " 的荣耀点数设置为 " + gp + "。");
        PacketSendUtility.sendMessage(target, "管理员将你的荣耀点数设置为 " + gp + "。");
      }
    } else if (params[0].equals("level")) {
      int level;
      try {
        level = Integer.parseInt(paramValue);
      } catch (NumberFormatException e) {
        PacketSendUtility.sendMessage(admin, "你需要输入有效的第二个参数！");
        return;
      }

      Player player = target;

      if (level <= GSConfig.PLAYER_MAX_LEVEL)
        player.getCommonData().setLevel(level);

      PacketSendUtility.sendMessage(admin, "已将 " + player.getCommonData().getName() + " 的等级设置为 " + player.getLevel());
    } else if (params[0].equals("title")) {
      int titleId;
      try {
        titleId = Integer.parseInt(paramValue);
      } catch (NumberFormatException e) {
        PacketSendUtility.sendMessage(admin, "你需要输入有效的第二个参数！");
        return;
      }

      Player player = target;
      if (titleId <= 160)
        setTitle(player, titleId);
      PacketSendUtility.sendMessage(admin, "已将 " + player.getCommonData().getName() + " 的称号设置为 " + titleId);

    }
  }

  private void setTitle(Player player, int value) {
    PacketSendUtility.sendPacket(player, new SM_TITLE_INFO(value));
    PacketSendUtility.broadcastPacket(player, (new SM_TITLE_INFO(player, value)));
    player.getCommonData().setTitleId(value);
  }

  @Override
  public void info(Player player, String message) {
    PacketSendUtility.sendMessage(player, "语法: //set <class|exp|ap|gp|level|title|help> <值>");
    PacketSendUtility.sendMessage(player, "说明: 设置选定玩家的职业、经验值、深渊点数、荣耀点数、等级或称号。");
    PacketSendUtility.sendMessage(player, "      使用//set help可查看此帮助信息。");
  }
}