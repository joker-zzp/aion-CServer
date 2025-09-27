package admincommands;

import java.util.List;

import com.aionemu.gameserver.ai.AIState;
import com.aionemu.gameserver.ai.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Rolandas, Neon
 */
public class MapCommand extends AdminCommand {

  public MapCommand() {
    super("map", "为当前地图实例提供不同功能。");

    // @formatter:off
    setSyntaxInfo(
      "<freeze|unfreeze> - (解)冻结当前地图实例上的所有NPC。",
      "<stats> - 显示所有地图的最高移动广播计数。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    if ("freeze".equalsIgnoreCase(params[0])) {
      List<Npc> npcs = admin.getPosition().getWorldMapInstance().getNpcs();
      npcs.forEach(npc -> npc.getAi().onGeneralEvent(AIEventType.FREEZE));
      sendInfo(admin, "世界地图已冻结！");
        long walkerCount = npcs.stream().filter(o -> o.getAi().getState() == AIState.WALKING).count();
        sendInfo(admin, "地图 " + admin.getPosition().getWorldMapInstance().getMapId() + " 上还剩 " + walkerCount + " 个行走者");
    } else if ("unfreeze".equalsIgnoreCase(params[0])) {
      admin.getPosition().getWorldMapInstance().forEachNpc(npc -> npc.getAi().onGeneralEvent(AIEventType.UNFREEZE));
      sendInfo(admin, "世界地图已解冻！");
    } else if ("stats".equalsIgnoreCase(params[0])) {
      for (String line : MovementNotifyTask.getInstance().dumpBroadcastStats())
        sendInfo(admin, line);
    }
  }
}