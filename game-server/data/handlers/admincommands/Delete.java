package admincommands;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Luno, Bobobear, Neon
 */
public class Delete extends AdminCommand {

  public Delete() {
    super("delete", "从世界中移除生成点。");

    // @formatter:off
    setSyntaxInfo(
      " - 删除您当前选中的对象。",
      "<range> - 删除指定半径范围内的所有对象（单位：米）。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      if (admin.getTarget() == null)
        sendInfo(admin);
      else
        delete(admin, admin.getTarget(), true);
    } else {
      int[] count = { 0 };
      float range = Float.parseFloat(params[0]);
      admin.getKnownList().forEachObject(object -> {
        if (PositionUtil.isInRange(admin, object, range) && delete(admin, object, false))
          count[0]++;
      });
      sendInfo(admin, "已删除 " + count[0] + (count[0] == 1 ? " 个对象。" : " 个对象。"));
    }
  }

  private boolean delete(Player admin, VisibleObject target, boolean notifyOnFail) {
    if (!(target instanceof Npc) && !(target instanceof Gatherable) && !(target instanceof HouseObject)) {
      if (notifyOnFail)
        PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
      return false;
    }

    SpawnTemplate spawn = target.getSpawn();
    if (spawn != null) { // 房屋对象没有生成模板
      if (spawn.hasPool()) {
        if (notifyOnFail)
          sendInfo(admin, "无法删除池化生成模板。");
        return false;
      }

      if (!spawn.getClass().equals(SpawnTemplate.class)) {
        if (notifyOnFail)
          sendInfo(admin, "无法删除特殊生成点 (生成类型: " + spawn.getClass().getSimpleName().replace("Template", "") + ").");
        return false;
      }
    }

    target.getController().delete();
    if (DataManager.SPAWNS_DATA.saveSpawn(target, true))
      sendInfo(admin, "生成点已永久移除。 " + target.getClass().getSimpleName() + " 将不会在服务器重启时再次生成。");
    return true;
  }
}