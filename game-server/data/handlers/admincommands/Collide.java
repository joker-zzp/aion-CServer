package admincommands;

import java.util.Iterator;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.geo.GeoService;

/**
 * @author Rolandas
 */
public class Collide extends AdminCommand {

  public Collide() {
    super("collide", "地形调试工具.");

    // @formatter:off
    setSyntaxInfo(
      " - 列出目标与地面之间的碰撞.",
      "me - 列出您与目标之间的碰撞."
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    boolean isMe = false;
    if (params.length > 0 && !(isMe = "me".equalsIgnoreCase(params[0]))) {
      sendInfo(admin);
      return;
    }
    VisibleObject target = admin.getTarget();
    if (target == null) {
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
      return;
    }

    final byte intentions = CollisionIntention.PHYSICAL.getId();
    float x = target.getX();
    float y = target.getY();
    float z = target.getZ();
    float targetX, targetY, targetZ;

    if (!isMe) {
      targetX = x;
      targetY = y;
      targetZ = z - 10;
    } else {
      targetX = admin.getX();
      targetY = admin.getY();
      targetZ = admin.getZ() + admin.getObjectTemplate().getBoundRadius().getUpper() / 6;
      sendInfo(admin, "从目标指向您:");
    }

    sendInfo(admin, "目标: X=" + x + "; Y=" + y + "; Z=" + z);

    CollisionResults results = GeoService.getInstance().getCollisions(target, targetX, targetY, targetZ, intentions, null);
    CollisionResult closest = results.getClosestCollision();

    if (results.size() == 0) {
      sendInfo(admin, "未找到碰撞。");
      closest = null;
    } else {
      listCollisions(admin, results, closest);
    }

    CollisionResult closestOpposite = null;

    if (isMe) {
      sendInfo(admin, "从您指向目标:");
        sendInfo(admin, "管理员: X=" + admin.getX() + "; Y=" + admin.getY() + "; Z=" + admin.getZ());

      results = GeoService.getInstance().getCollisions(admin, target.getX(), target.getY(),
        target.getZ() + target.getObjectTemplate().getBoundRadius().getUpper() / 2, intentions, null);
      closestOpposite = results.getClosestCollision();

      if (results.size() == 0) {
        sendInfo(admin, "未找到碰撞。");
        closestOpposite = null;
      } else {
        listCollisions(admin, results, closestOpposite);
      }
    }

    if (!isMe && closest != null && closest.getContactPoint().z + 0.5f < target.getZ()) {
      sendInfo(admin, "最近的碰撞点在目标Z坐标下方！");
    } else {
      if (closest != null) {
        SpawnTemplate spawn = SpawnEngine.newSpawn(admin.getWorldId(), 200000, closest.getContactPoint().x, closest.getContactPoint().y,
          closest.getContactPoint().z, (byte) 0, 0);
        SpawnEngine.spawnObject(spawn, admin.getInstanceId());
      }
      if (closestOpposite != null) {
        SpawnTemplate spawn = SpawnEngine.newSpawn(admin.getWorldId(), 200000, closestOpposite.getContactPoint().x,
          closestOpposite.getContactPoint().y, closestOpposite.getContactPoint().z, (byte) 0, 0);
        SpawnEngine.spawnObject(spawn, admin.getInstanceId());
      }
    }
  }

  private void listCollisions(Player admin, CollisionResults results, CollisionResult closestOpposite) {
    int count = 1;
    int closestId = 0;
    String description = "";

    for (Iterator<CollisionResult> iter = results.iterator(); iter.hasNext(); count++) {
      CollisionResult result = iter.next();
      if (result.equals(closestOpposite))
        closestId = count;
      if (result.getGeometry() == null)
        description += count + ". " + result.getContactPoint().toString() + "\n";
      else {
        if (result.getGeometry().getName() == null) {
          description += count + ". " + result.getContactPoint().toString() + "; parent=" + result.getGeometry().getParent().getName() + "\n";
        } else
          description += count + ". " + result.getContactPoint().toString() + "; name=" + result.getGeometry().getName() + "\n";
      }
    }
    description += "-----------------------\n最近碰撞: " + closestId + ". 距离: " + closestOpposite.getDistance();
    sendInfo(admin, description);
  }
}