package admincommands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapType;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * @author Neon
 */
public class MoveTo extends AdminCommand {

  public MoveTo() {
    super("moveto", "将您传送到任意位置.");

    // @formatter:off
    setSyntaxInfo(
      "<x> <y> [z] - 将您传送到当前地图上的指定坐标(也支持粘贴的xml属性, 如x=\"1422.7744\" y=\"1250.0612\" z=\"569.47\").",
      "<地图名称|ID> <x> <y> [z] - 将您传送到指定位置(地图名称需要使用下划线替代空格).",
      "<位置链接> - 将您传送到聊天链接中的位置.",
      "<玩家名称> - 将您传送到该玩家的位置.",
      "<NPC名称|ID> - 将您传送到该NPC的位置."
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    String errorMsg = null;

    if (params.length >= 1) {
      WorldPosition pos;
      if (params.length == 1) {
        pos = ChatUtil.getPosition(params[0]);
      } else {
        pos = parseWorldPosition(admin, params);
      }
      if (pos != null) {
        pos.setH(admin.getHeading());
        moveTo(admin, pos, "已传送到 " + WorldMapType.getWorld(pos.getMapId()) + "\nX:" + pos.getX() + " Y:" + pos.getY() + " Z:" + pos.getZ());
        return;
      } else if (params.length > 1 || params[0].startsWith("[pos:"))
      errorMsg = "无效的地图位置或缺少/已禁用的地形数据.";
    }

    if (params.length == 1 && !NumberUtils.isDigits(params[0])) {
      Player player = World.getInstance().getPlayer(Util.convertName(params[0]));
      // 将参数专程字符输出
      sendInfo(admin, "params:" + params[0]);
      // 将参数中的数字提取出来
      Pattern p = Pattern.compile("\\d+");
      Matcher m = p.matcher(params[0]);
      if (m.find()) {
        params[0] = m.group();
      }
      int npcId = getNpcId(admin, params);
      if (player != null && !player.equals(admin)) {
        moveTo(admin, player.getPosition(), "已传送到 " + ChatUtil.name(player) + "。");
        return;
      } else if (npcId > 0) {
        sendInfo(admin, "已传送到 " + ChatUtil.path(npcId, true) + "。");
        TeleportService.teleportToNpc(admin, npcId);
        return;
      } else if (errorMsg == null && player != null) {
        errorMsg = "无效的玩家名称或玩家不在线.";
      } else if (errorMsg == null && npcId == 0) {
        errorMsg = "无效的NPC链接ID。";
      } else if (errorMsg == null) {
        errorMsg = "无效的参数.";
      }
    }
    
    if (params.length == 0) {
      // 检查玩家的目标是否存在
      if (admin.getTarget() != null) {
        moveTo(admin, admin.getTarget().getPosition(), "已传送到 " + ChatUtil.name(admin.getTarget()) + ".");
        return;
      }
      errorMsg = "目标不存在.";
    }

    sendInfo(admin, errorMsg);
  }

  private WorldPosition parseWorldPosition(Player admin, String[] params) {
    int coordIndex = 0;
    int mapId;
    boolean isMapNameOrId = params[0].matches("^([a-zA-Z_]+|[1-9][0-9]{8,})$");
    if (isMapNameOrId) {
      mapId = NumberUtils.toInt(params[0]);
      if (mapId == 0)
        mapId = WorldMapType.getMapId(params[0]);
      coordIndex = 1;
    } else {
      mapId = admin.getPosition().getMapId();
    }
    Float x = null, y = null, z = null;
    Pattern p = Pattern.compile("^((?<type>x|y|z)(=|:)\"?)?(?<coord>[0-9]+(\\.[0-9]+)?f?)\"?,?$", Pattern.CASE_INSENSITIVE);
    int maxIndex = Math.min(params.length, coordIndex + 3);
    for (int i = coordIndex; i < maxIndex; i++) {
      Matcher m = p.matcher(params[i]);
      if (m.find()) {
        float coord = NumberUtils.toFloat(m.group("coord"));
        String type = m.group("type");
        if ("x".equalsIgnoreCase(type) || x == null && type == null)
          x = coord;
        else if ("y".equalsIgnoreCase(type) || y == null && type == null)
          y = coord;
        else if ("z".equalsIgnoreCase(type) || z == null && type == null)
          z = coord;
      } else {
        return null;
      }
    }
    return x == null || y == null ? null : ChatUtil.parsedCoordsToWorldPosition(mapId, x, y, z, null);
  }

  private void moveTo(Player admin, WorldPosition pos, String message) {
    sendInfo(admin, message); // msg before teleport, otherwise client could ignore it
    if (pos.isInstanceMap() && pos.getWorldMapInstance().getParent().getMainWorldMapInstance() == pos.getWorldMapInstance()) {
      // currently instance type maps have a main world map instance (default) which have no spawns, so we create a new instance instead
      WorldMapInstance instance = InstanceService.getOrRegisterInstance(pos.getMapId(), admin);
      pos = World.getInstance().createPosition(pos.getMapId(), pos.getX(), pos.getY(), pos.getZ(), pos.getHeading(), instance.getInstanceId());
    }
    TeleportService.teleportTo(admin, pos);
  }

  private int getNpcId(Player admin, String... params) {
    if (NumberUtils.isDigits(params[0])) {
      int npcId = NumberUtils.toInt(params[0]);
      if (npcId > 0 && DataManager.SPAWNS_DATA.getFirstSpawnByNpcId(admin.getWorldId(), npcId) != null)
        return npcId;
    } else {
      String npcName = String.join(" ", params).toLowerCase();
      for (NpcTemplate template : DataManager.NPC_DATA.getNpcData()) {
        if (template.getName().toLowerCase().equals(npcName)) {
          if (DataManager.SPAWNS_DATA.getFirstSpawnByNpcId(admin.getWorldId(), template.getTemplateId()) != null)
            return template.getTemplateId();
        }
      }
    }
    return 0;
  }
}