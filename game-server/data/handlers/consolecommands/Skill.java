package consolecommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GM_SHOW_PLAYER_SKILLS;
import com.aionemu.gameserver.network.aion.skillinfo.SkillEntryWriter;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.ConsoleCommand;
import com.aionemu.gameserver.utils.collections.DynamicServerPacketBodySplitList;
import com.aionemu.gameserver.utils.collections.SplitList;
import com.aionemu.gameserver.world.World;
import java.util.List;
import java.util.ArrayList;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.skillengine.model.SkillLearnTemplate;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Persistable;

/**
 * @author Yeats
 */
public class Skill extends ConsoleCommand {

  public Skill() {
    super("skill", "显示玩家技能");

    setSyntaxInfo(
      "skill <player> - 显示玩家技能",
      "skill <job> - 显示职业技能列表"
    );
  }

  @Override
  protected void execute(Player admin, String... params) {
    if (params.length > 0) {
      try {
        // 尝试将参数解析为jobId
        int jobId = Integer.parseInt(params[0]);
        showJobSkills(admin, jobId);
        return;
      } catch (NumberFormatException e) {
        // 如果参数不是数字，则继续尝试作为玩家名称
      }
      
      // 尝试查找玩家
      Player target = World.getInstance().getPlayer(params[0]);
      if (target != null) {
        showPlayerSkills(admin, target);
        return;
      }
    }
    
    // 检查目标是否是玩家
    if (admin.getTarget() instanceof Player player) {
      showPlayerSkills(admin, player);
    } else {
      // 显示帮助信息
      PacketSendUtility.sendMessage(admin, getSyntaxInfo());
    }
  }

  // 显示玩家技能
  private void showPlayerSkills(Player admin, Player target) {
    SplitList<PlayerSkillEntry> skillEntrySplitList = new DynamicServerPacketBodySplitList<>(target.getSkillList().getAllSkills(), false,
      SM_GM_SHOW_PLAYER_SKILLS.STATIC_BODY_SIZE, SkillEntryWriter.DYNAMIC_BODY_PART_SIZE_CALCULATOR);
    skillEntrySplitList.forEach(part -> PacketSendUtility.sendPacket(admin, new SM_GM_SHOW_PLAYER_SKILLS(part)));
  }

  // 显示职业技能列表
  private void showJobSkills(Player admin, int jobId) {
    try {
      // 根据jobId获取PlayerClass对象
      PlayerClass playerClass = PlayerClass.getPlayerClassById((byte) jobId);
      
      // 获取管理员的种族（用于过滤种族特定的技能）
      Race race = admin.getRace();
      
      // 创建一个列表来存储所有职业技能
      List<PlayerSkillEntry> skillEntries = new ArrayList<>();
      
      // 遍历多个等级来获取该职业的所有技能
      // 这里从1级到最高等级（假设为65级）
      for (int level = 1; level <= 65; level++) {
        List<SkillLearnTemplate> skillTemplates = DataManager.SKILL_TREE_DATA.getTemplatesFor(playerClass, level, race);
        for (SkillLearnTemplate template : skillTemplates) {
          // 创建一个虚拟的PlayerSkillEntry来表示这个技能
          // 使用正确的构造函数：skillId, skillLvl, skillType, persistentState
          int skillType = template.isStigma() ? (template.isLinkedStigma() ? 3 : 1) : 0;
          PlayerSkillEntry entry = new PlayerSkillEntry(
            template.getSkillId(), 
            template.getSkillLevel(), 
            skillType, 
            Persistable.PersistentState.NOACTION
          );
          skillEntries.add(entry);
        }
      }
      
      // 使用与showPlayerSkills相同的方式发送技能列表
      SplitList<PlayerSkillEntry> skillEntrySplitList = new DynamicServerPacketBodySplitList<>(skillEntries, false,
        SM_GM_SHOW_PLAYER_SKILLS.STATIC_BODY_SIZE, SkillEntryWriter.DYNAMIC_BODY_PART_SIZE_CALCULATOR);
      skillEntrySplitList.forEach(part -> PacketSendUtility.sendPacket(admin, new SM_GM_SHOW_PLAYER_SKILLS(part)));
      
      // 发送提示信息
      PacketSendUtility.sendMessage(admin, "已显示职业 " + playerClass.name() + " 的所有技能列表");
    } catch (IllegalArgumentException e) {
      // 处理无效的jobId
      PacketSendUtility.sendMessage(admin, "无效的职业ID: " + jobId);
    } catch (Exception e) {
      PacketSendUtility.sendMessage(admin, "获取职业技能列表时发生错误");
      e.printStackTrace();
    }
  }
}
