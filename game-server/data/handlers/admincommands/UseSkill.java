package admincommands;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 使用技能命令类 - 允许管理员使用或让目标使用任何技能，包括技能列表中没有的技能
 * @author Source, kecimis, Estrayl, Neon
 */
public class UseSkill extends AdminCommand {

  public UseSkill() {
    super("useskill", "使用（或让目标使用）任何技能，包括技能列表中没有的技能");

    // @formatter:off
    setSyntaxInfo(
      "help - 显示使用技能命令的帮助信息",
      "<id> [lvl] [f] - 以指定的技能等级在你的目标上使用技能（f = 强制使用）",
      "<me|self|target> <id> [lvl] [f] - 让你的目标在你、自身或其目标上使用技能（f = 强制使用）"
    );
    // @formatter:on
  }

  @Override
  protected void execute(Player admin, String... params) {
    if (params.length == 0 || (params.length == 1 && params[0].equalsIgnoreCase("help"))) {
      sendInfo(admin);
      return;
    }

    try {
      String targetMode = params[0].toLowerCase();
      int i = 0;
      switch (targetMode) {
        case "me":
        case "self":
        case "target":
          i++;
          break;
        default:
          targetMode = null;
      }
      SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(Integer.parseInt(params[i++]));
      if (template != null) {
        int skillLevel = params.length > i && NumberUtils.isNumber(params[i]) ? Integer.parseInt(params[i++]) : template.getLvl();
        boolean forceUse = params.length > i && params[i].equals("f");
        if (useSkill(admin, template, skillLevel, targetMode, forceUse))
        sendInfo(admin, "使用技能: " + template.getL10n());
      else
        sendInfo(admin, "无法使用技能（" + (forceUse ? "缺少前置条件" : "添加参数 'f' 以强制使用") + "）");
    } else {
      sendInfo(admin, "无效的技能ID");
    }
  } catch (NumberFormatException e) {
      sendInfo(admin, "无效的技能ID或等级");
    }
  }

  private boolean useSkill(Player player, SkillTemplate template, int skillLevel, String targetMode, boolean forceUse) {
    Creature effector;
    VisibleObject target;
    if (targetMode != null) {
      if (!(player.getTarget() instanceof Creature creatureTarget)) {
        PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
        return false;
      }
      effector = creatureTarget;
      target = getTarget(player, targetMode);
    } else {
      effector = player;
      target = player.getTarget();
    }

    Skill skill = SkillEngine.getInstance().getSkill(effector, template.getSkillId(), skillLevel, target);
    if (skill != null)
      return forceUse ? skill.useWithoutPropSkill() : skill.useNoAnimationSkill();

    return false;
  }

  private VisibleObject getTarget(Player player, String targetMode) {
    return switch (targetMode) {
      case "me" -> player;
      case "self" -> player.getTarget();
      case "target" -> player.getTarget() == null ? null : player.getTarget().getTarget();
      default -> null;
    };
  }
}