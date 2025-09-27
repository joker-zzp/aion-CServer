package admincommands;

import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.Drop;
import com.aionemu.gameserver.model.drop.DropGroup;
import com.aionemu.gameserver.model.drop.DropModifiers;
import com.aionemu.gameserver.model.drop.NpcDrop;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.globaldrops.GlobalDropItem;
import com.aionemu.gameserver.model.templates.globaldrops.GlobalRule;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.event.EventService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldDropType;

/**
 * @author Oliver, AionCool, Bobobear, Neon
 */
public class DropInfo extends AdminCommand {

  public DropInfo() {
    super("dropinfo", "显示目标的掉落信息.");

    setSyntaxInfo("[all] - 列出选中NPC的掉落物 (默认: 仅显示您等级范围内的掉落, 可选参数: 所有可能的掉落).");
  }

  @Override
  public void execute(Player player, String... params) {
    VisibleObject visibleObject = player.getTarget();

    if (!(visibleObject instanceof Npc npc)) {
      sendInfo(player);
      return;
    }

    boolean showAll = params.length > 0 && params[0].equals("all");
    NpcDrop npcDrop = DataManager.CUSTOM_NPC_DROP.getNpcDrop(npc.getNpcId());
    DropModifiers dropModifiers = DropRegistrationService.getInstance().createDropModifiers(npc, player, player.getLevel());
    dropModifiers.setMaxDropsPerGroup(Integer.MAX_VALUE);

    int[] counts = { 0, 0 };
    String info = "[" + npc.getObjectTemplate().getL10n() + "的掉落]";
    if (npcDrop != null) {
      for (DropGroup dropGroup : npcDrop.getDropGroup()) {
        if (dropGroup.getRace() == Race.PC_ALL || dropGroup.getRace() == dropModifiers.getDropRace()) {
          info += "\n自定义掉落组: " + dropGroup.getName() + ", 最大掉落数: " + dropGroup.getMaxItems();
          counts[1]++;
          for (Drop drop : dropGroup.getDrop()) {
            float finalChance = dropModifiers.calculateDropChance(drop.getChance(), dropGroup.isUseLevelBasedChanceReduction());
            if (!showAll && finalChance <= 0)
              continue;
            info += "\t" + ChatUtil.item(drop.getItemId()) + "\t基础概率: " + drop.getChance() + "%, 有效概率: " + finalChance + "%";
            counts[0]++;
          }
        }
      }
    }

    // if npc ai == quest_use_item it will be always excluded from global drops
    boolean isNpcQuest = npc.getAi().getName().equals("quest_use_item");
    if (!isNpcQuest) {
      boolean hasGlobalNpcExclusions = DropRegistrationService.getInstance().hasGlobalNpcExclusions(npc);
      boolean isAllowedDefaultGlobalDropNpc = DropRegistrationService.getInstance().isAllowedDefaultGlobalDropNpc(npc, dropModifiers.isDropNpcChest());
      // instances with WorldDropType.NONE must not have global drops (example Arenas)
      if (!hasGlobalNpcExclusions && npc.getWorldDropType() != WorldDropType.NONE) {
        info += collectDropInfo("全局", DataManager.GLOBAL_DROP_DATA.getAllRules(), npc, dropModifiers, isAllowedDefaultGlobalDropNpc, showAll,
          counts);
      }
      if (!hasGlobalNpcExclusions || dropModifiers.isDropNpcChest())
        info += collectDropInfo("活动", EventService.getInstance().getActiveEventDropRules(), npc, dropModifiers, isAllowedDefaultGlobalDropNpc,
          showAll, counts);
    }

    info += "\n" + counts[0] + " 个总掉落物存在于 " + counts[1] + " 个掉落组中" + (showAll ? "." : " (基于您的等级)");
    sendInfo(player, info);
  }

  private String collectDropInfo(String dropGroupPrefix, List<GlobalRule> rules, Npc npc, DropModifiers dropModifiers,
      boolean isAllowedDefaultGlobalDropNpc, boolean showAll, int[] counts) {
    String info = "";
    for (GlobalRule rule : rules) {
      // if getGlobalRuleNpcs() != null means drops are for specified npcs (like named drops) so the default restrictions will be ignored
      if (isAllowedDefaultGlobalDropNpc || rule.getGlobalRuleNpcs() != null) {

        float chance = DropRegistrationService.getInstance().calculateEffectiveChance(rule, npc, dropModifiers);
        if (!showAll && chance <= 0)
          continue;

        List<GlobalDropItem> drops = DropRegistrationService.getInstance().collectDrops(rule, npc, dropModifiers);
        if (!drops.isEmpty()) {
          info += "\n" + dropGroupPrefix + "掉落组: \"" + rule.getRuleName() + "\", 最大掉落数: " + rule.getMaxDropRule();
          if (rule.getMemberLimit() != 1)
            info += ", 成员限制: " + rule.getMemberLimit();
          info += "\t基础概率: " + rule.getChance() + "%, 有效概率: " + chance + "%";
          counts[1]++;
          for (GlobalDropItem item : drops) {
            info += "\t" + ChatUtil.item(item.getId());
            if (item.getChance() != 100f)
              info += "\t子概率: " + item.getChance() + "%";
            counts[0]++;
          }
        }
      }
    }
    return info;
  }
}