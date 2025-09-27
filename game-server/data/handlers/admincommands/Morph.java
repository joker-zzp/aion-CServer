package admincommands;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author ATracer, aionchs-, Wylovech, Neon
 */
public class Morph extends AdminCommand {

  public Morph() {
    super("morph", "将玩家变形为任何NPC。");

    setSyntaxInfo(
      " - 将你变形为你当前选中的NPC。",
      "<id> - 将你的目标变形为指定的NPC（0为取消变形）。"
    );
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0 && !(admin.getTarget() instanceof Npc)) {
      sendInfo(admin);
      return;
    }

    Player target = admin.getTarget() instanceof Player p ? p : admin;
    int npcId;

    if (params.length == 0 && admin.getTarget() instanceof Npc npc) {
      npcId = npc.getNpcId();
    } else {
      try {
        npcId = Integer.parseInt(params[0]);
      } catch (NumberFormatException e) {
        sendInfo(admin);
        return;
      }
    }

    if (npcId < 0 || npcId > 0 && npcId < 200000) {
      sendInfo(admin, "无效的ID。");
      return;
    }

    target.getTransformModel().apply(npcId);

    if (npcId == 0) {
      sendInfo(admin, "已取消" + (target.equals(admin) ? "" : " " + target.getName() + "的") + "变形。");
    } else {
      sendInfo(admin, "你已将" + (target.equals(admin) ? "自己" : " " + target.getName()) + "变形为" + ChatUtil.path(npcId, true) + "。");
      if (!target.equals(admin))
        sendInfo(target, ChatUtil.name(admin) + "将你变形为NPC形态。");
    }
  }
}