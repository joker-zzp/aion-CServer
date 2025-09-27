package admincommands;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import consolecommands.Clearusercoolt;

/**
 * @author kecimis
 */
public class RemoveCd extends AdminCommand {

  public RemoveCd() {
    super("removecd", "清除技能、物品和副本的冷却时间。");

    // @formatter:off
    setSyntaxInfo(
      " - 移除目标所有物品和技能的冷却时间。",
      "<instance> <all|worldId> - 移除目标指定副本的冷却时间。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length > 0 && "help".equals(params[0])) {
      sendInfo(admin);
      return;
    }
    
    VisibleObject target = admin.getTarget();
    if (target == null)
      target = admin;

    if (target instanceof Player player) {
      if (params.length == 0) {
        if (player.getSkillCoolDowns() != null) {
          long currentTime = System.currentTimeMillis();
          for (Entry<Integer, Long> en : player.getSkillCoolDowns().entrySet())
            player.setSkillCoolDown(en.getKey(), currentTime);
          PacketSendUtility.sendPacket(player, new SM_SKILL_COOLDOWN(player.getSkillCoolDowns()));
        }

        Map<Integer, ItemCooldown> dummyCds = new HashMap<>(); // 4.8客户端忽略reuseTime <= currentTime，但发送旧cds + useDelay 0有效
        for (Entry<Integer, ItemCooldown> en : player.getItemCoolDowns().entrySet()) {
          dummyCds.put(en.getKey(), new ItemCooldown(en.getValue().getReuseTime(), 0));
          player.removeItemCoolDown(en.getKey());
        }
        PacketSendUtility.sendPacket(player, new SM_ITEM_COOLDOWN(dummyCds));

        player.getHouseObjectCooldowns().clear();

        if (player.equals(admin)) {
          sendInfo(admin, "您的物品和技能冷却时间已移除。");
        } else {
          sendInfo(admin, "您已移除" + player.getName() + "的物品和技能冷却时间。");
          sendInfo(player, admin.getName(true) + "移除了您的物品和技能冷却时间。");
        }
      } else if (params[0].equalsIgnoreCase("instance") && params.length >= 2) {
        if (params[1].equalsIgnoreCase("all")) {
          Clearusercoolt.clearAllInstanceCooldowns(admin, player);
        } else {
          int worldId = Integer.parseInt(params[1]);
          if (player.getPortalCooldownList().isPortalUseDisabled(worldId)) {
            player.getPortalCooldownList().removePortalCooldown(worldId);
            player.getPortalCooldownList().sendEntryInfo(worldId);

            String worldName = World.getInstance().getWorldMap(worldId).getName().replace('_', ' ');
            if (player.equals(admin)) {
              sendInfo(admin, "您在" + worldName + "的副本冷却时间已移除。");
            } else {
              sendInfo(admin, "您已移除" + player.getName() + "在" + worldName + "的副本冷却时间。");
              sendInfo(player, admin.getName(true) + "移除了您在" + worldName + "的副本冷却时间");
            }
          } else
            sendInfo(admin, (player.equals(admin) ? "您" : player.getName()) + "在此副本上没有冷却时间。");

        }
      } else {
        sendInfo(admin);
      }
    } else
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
  }
}