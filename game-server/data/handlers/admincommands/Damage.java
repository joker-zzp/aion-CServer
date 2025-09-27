package admincommands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Source
 */
public class Damage extends AdminCommand {

  public Damage() {
    super("damage", "伤害控制命令");
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length > 2)
      info(admin, null);

    VisibleObject target = admin.getTarget();
    if (target == null)
      PacketSendUtility.sendMessage(admin, "未选择目标");
    else if (target instanceof Creature) {
      Creature creature = (Creature) target;
      int dmg;
      String damageType = "hp";
      boolean isPercent = false;
      if (params[0].equalsIgnoreCase("mp")) {
        damageType = "mp";
      } else if (params[0].equalsIgnoreCase("fp")) {
        damageType = "fp";
      } else if (params[0].equalsIgnoreCase("dp")) {
        damageType = "dp";
      }

      if (damageType.equalsIgnoreCase("fp") || damageType.equalsIgnoreCase("dp")) {
        if (!(creature instanceof Player)) {
          info(admin, null);
          return;
        }
      }

      if (!damageType.equalsIgnoreCase("hp") && params.length != 2) {
        info(admin, null);
        return;
      }

      try {
        String percent = params[0];
        if (!damageType.equalsIgnoreCase("hp"))
          percent = params[1];
        Pattern damage = Pattern.compile("([^%]+)%");
        Matcher result = damage.matcher(percent);

        if (result.find()) {
          dmg = Integer.parseInt(result.group(1));
          isPercent = true;
        } else if (damageType.equalsIgnoreCase("hp"))
          dmg = Integer.parseInt(params[0]);
        else
          dmg = Integer.parseInt(params[1]);

        if (dmg <= 100)
          isPercent = true;

        switch (damageType) {
          case "hp":
            if (isPercent)
              dmg = (int) (dmg / 100f * creature.getLifeStats().getMaxHp());
            creature.getController().onAttack(creature, dmg, null);
            break;
          case "mp":
            if (isPercent)
              dmg = (int) (dmg / 100f * creature.getLifeStats().getMaxMp());
            creature.getLifeStats().reduceMp(TYPE.DAMAGE_MP, dmg, 0, LOG.MPATTACK);
            break;
          case "fp":
            if (isPercent)
              dmg = (int) (dmg / 100f * ((Player) creature).getLifeStats().getMaxFp());
            ((Player) creature).getLifeStats().reduceFp(TYPE.FP_DAMAGE, dmg, 0, LOG.FPATTACK);
            break;
          case "dp":
            if (isPercent)
              dmg = (int) (dmg / 100f * 4000f);
            if (dmg > ((Player) creature).getCommonData().getDp())
              dmg = ((Player) creature).getCommonData().getDp();
            ((Player) creature).getCommonData().setDp(((Player) creature).getCommonData().getDp() - dmg);
            break;
        }
      } catch (Exception ex) {
        info(admin, null);
      }
    }
  }

  @Override
  public void info(Player player, String message) {
    PacketSendUtility.sendMessage(player, "语法 //damage (mp/fp/dp) <数值 | 百分比%>" + "\n<数值>必须是数字" + "\n(mp/fp/dp)是可选的，留空则使用HP伤害" + "\n使用fp/dp时，目标必须是玩家");
  }

}