package admincommands;

import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.SkillElement;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.stats.container.PlayerGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.restrictions.PlayerRestrictions;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.spawnengine.ClusteredNpc;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.PositionUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.stats.CalculationType;
import com.aionemu.gameserver.utils.stats.StatFunctions;

/**
 * @author Nemiroff, Neon
 */
public class Info extends AdminCommand {

  public Info() {
    super("info", "显示目标的详细信息。");
  }

  @Override
  public void execute(Player admin, String... params) {
    VisibleObject target = admin.getTarget();

    if (target == null) {
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
      return;
    }

    sendInfo(admin, "[关于 " + target.getClass().getSimpleName() + " 的信息]\n\t名称: " + target.getName() + ", 对象ID: " + target.getObjectId()
      + "\n\t模板ID: " + target.getObjectTemplate().getTemplateId());

    if (target instanceof Creature creature) {
      if (creature instanceof Player player) {
        Pet pet = player.getPet();
        sendInfo(admin, (pet != null ? "宠物ID: " + pet.getObjectTemplate().getTemplateId() + ", 对象ID: " + pet.getObjectId() + "\n\t" : "")
          + "城镇ID: " + TownService.getInstance().getTownResidence(player));
        sendInfo(admin, "当前帕纳斯特拉阵营: %s".formatted(player.getPanesterraFaction()));
        PlayerGameStats pgs = player.getGameStats();
        sendInfo(admin,
          "[属性]"
              + "\n\tHP: " +  player.getLifeStats().getCurrentHp() +  "/" + pgs.getMaxHp().getCurrent()
              + ", MP: " + player.getLifeStats().getCurrentMp() + "/" + pgs.getMaxMp().getCurrent()
              + ", 飞行值: " + player.getLifeStats().getCurrentFp() + "/" + pgs.getFlyTime().getCurrent()
              + ", DP: " + player.getCommonData().getDp() + "/" + pgs.getMaxDp().getCurrent()
              + "\n\t力量: " + pgs.getPower().getCurrent()
              + ", 体力: " + pgs.getHealth().getCurrent()
              + ", 敏捷: " + pgs.getAgility().getCurrent()
              + ", 命中: " + pgs.getAccuracy().getCurrent()
              + ", 智力: " + pgs.getKnowledge().getCurrent()
              + ", 意志: " + pgs.getWill().getCurrent()
              + "\n\t施法速度加成: " + (pgs.getStat(StatEnum.BOOST_CASTING_TIME, 1000).getCurrent() * 0.1f - 100) + "%"
              + "\n\t基础攻击速度: " + pgs.getAttackSpeed().getBase() * 0.001f
              + "\n\t当前攻击速度: " + pgs.getAttackSpeed().getCurrent() * 0.001f
              + "\n\t移动速度: " + pgs.getMovementSpeedFloat()
              + "\n\t-------------攻击属性-------------"
              + "\n\t魔法增幅: " + pgs.getMBoost().getCurrent()
              + "\n\t魔法命中: " + pgs.getMAccuracy().getCurrent()
              + "\n\t魔法暴击: " + pgs.getMCritical().getCurrent()
              + "\n\t\t---------主手-----------"
              + "\n\t\t魔法攻击力: " + (pgs.getMainHandMAttack(CalculationType.DISPLAY).getCurrent())
              + "\n\t\t物理攻击力: " + pgs.getMainHandPAttack(CalculationType.DISPLAY).getCurrent()
              + "\n\t\t物理命中: " + pgs.getMainHandPAccuracy().getCurrent()
              + "\n\t\t物理暴击: " + pgs.getMainHandPCritical().getCurrent()
              + "\n\t\t-----------副手-----------"
              + "\n\t\t显示魔法攻击力: " + (pgs.getOffHandMAttack(CalculationType.DISPLAY).getCurrent())
              + ", 最小值: " + (int) (pgs.getOffHandMAttack().getCurrent() * pgs.getMinDamageRatio())
              + ", 最大值: " + pgs.getOffHandMAttack().getCurrent()
              + "\n\t\t显示物理攻击力: " + (pgs.getOffHandPAttack(CalculationType.DISPLAY).getCurrent())
              + ", 最小值: " + (int) (pgs.getOffHandPAttack().getCurrent() * pgs.getMinDamageRatio())
              + ", 最大值: " + pgs.getOffHandPAttack().getCurrent()
              + "\n\t\t物理命中: " + pgs.getOffHandPAccuracy().getCurrent()
              + "\n\t\t物理暴击: " + pgs.getOffHandPCritical().getCurrent()
              + "\n\t-------------防御属性--------------"
              + "\n\t\t魔法防御: " + pgs.getMDef().getCurrent()
              + "\n\t\t魔法抵抗: " + pgs.getMResist().getCurrent()
              + "\n\t\t魔法暴击抵抗: " + pgs.getMCR()
              + "\n\t\t魔法暴击防御: " + pgs.getStat(StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE, 0).getCurrent()
              + "\n\t\t物理防御: " + pgs.getPDef().getCurrent()
              + "\n\t\t格挡: " + pgs.getBlock().getCurrent()
              + "\n\t\t回避: " + pgs.getParry().getCurrent()
              + "\n\t\t闪躲: " + pgs.getEvasion().getCurrent()
              + "\n\t\t物理暴击抵抗: " + pgs.getPCR().getCurrent()
              + "\n\t\t物理暴击防御: " + pgs.getStat(StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 0).getCurrent()
              + "\n\t\t风属性抵抗: " + pgs.getMagicalDefenseFor(SkillElement.WIND)
              + "\n\t\t水属性抵抗: " + pgs.getMagicalDefenseFor(SkillElement.WATER)
              + "\n\t\t地属性抵抗: " + pgs.getMagicalDefenseFor(SkillElement.EARTH)
              + "\n\t\t火属性抵抗: " + pgs.getMagicalDefenseFor(SkillElement.FIRE)
              + "\n\t\t暗属性抵抗: " + pgs.getMagicalDefenseFor(SkillElement.DARK)
              + "\n\t\t光属性抵抗: " + pgs.getMagicalDefenseFor(SkillElement.LIGHT)
              + "\n\t-------------PvP属性-------------"
              + "\n\tPvP攻击: " + pgs.getStat(StatEnum.PVP_ATTACK_RATIO, 0).getCurrent() * 0.1f + "%"
              + "\n\tPvP物理攻击: " + pgs.getStat(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 0).getCurrent() * 0.1f + "%"
              + "\n\tPvP魔法攻击: " + pgs.getStat(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 0).getCurrent() * 0.1f + "%"
              + "\n\tPvP防御: " + pgs.getStat(StatEnum.PVP_DEFEND_RATIO, 0).getCurrent() * 0.1f + "%"
              + "\n\tPvP物理防御: " + pgs.getStat(StatEnum.PVP_DEFEND_RATIO_PHYSICAL, 0).getCurrent() * 0.1f + "%"
              + "\n\tPvP魔法防御: " + pgs.getStat(StatEnum.PVP_DEFEND_RATIO_MAGICAL, 0).getCurrent() * 0.1f + "%");

        for (int i = 0; i < 2; i++) {
          NpcFaction faction = player.getNpcFactions().getActiveNpcFaction(i == 0);
          if (faction != null) {
            sendInfo(admin,
              player.getName() + " 已加入 " + (i == 0 ? "导师" : "每日") + " 势力: " + DataManager.NPC_FACTIONS_DATA.getNpcFactionById(faction.getId()).getName()
                  + "\n\t当前任务状态: " + faction.getState().name()
                  + (faction.getState().equals(ENpcFactionQuestState.COMPLETE) ? ("\n\t下次可接: " + ((faction.getTime() - System.currentTimeMillis() / 1000) / 3600f) + " 小时后") : ""));
          }
        }
      } else if (creature instanceof Npc npc) {
        sendInfo(admin, "[模板信息]\n\t评级: " + npc.getRating() + ", 等级: " + npc.getRank()
          + "\n\t模板类型: " + npc.getNpcTemplateType() + ", 深渊类型: " + npc.getAbyssNpcType()
          + "\n\t相对经验奖励: " + StatFunctions.calculateExperienceReward(admin.getLevel(), npc));
        if (npc instanceof SiegeNpc)
          sendInfo(admin, "[要塞信息]\n\t要塞ID: " + ((SiegeNpc) npc).getSiegeId() + ", 要塞种族: " + ((SiegeNpc) npc).getSiegeRace());
        sendInfo(admin,
          "[AI信息]\n\tAI: " + npc.getAi().getName()
              + "\n\t状态: " + npc.getAi().getState() + ", 子状态: " + npc.getAi().getSubState());
        sendInfo(admin,
          "[感知范围]\n\t半径: " + npc.getAggroRange()
              + "\n\t短半径: " + npc.getShortAggroRange()
              + "\n\t角度: " + npc.getAggroAngle()
              + "\n\t侧面: " + npc.getObjectTemplate().getBoundRadius().getSide() + ", 正面: " + npc.getObjectTemplate().getBoundRadius().getFront() + ", 上方: " + npc.getObjectTemplate().getBoundRadius().getUpper()
              + "\n\t方向边界: " + PositionUtil.getDirectionalBound(npc, admin, true)
              + "\n\t距离: " + (npc.getAggroRange() + PositionUtil.getDirectionalBound(npc, admin, true)));
        sendInfo(admin, "[生成信息]\n\t静态ID: " + npc.getSpawn().getStaticId() + ", 距生成点距离: " + npc.getDistanceToSpawnLocation() + "米");
        if (npc.isPathWalker()) {
          sendInfo(admin, "\t路线ID: " + npc.getSpawn().getWalkerId());
          if (npc.getWalkerGroup() != null) {
            ClusteredNpc snpc = npc.getWalkerGroup().getClusterData(npc);
            sendInfo(admin, "\t移动组类型: " + npc.getWalkerGroup().getWalkType() + ", X偏移: " + snpc.getXDelta() + ", Y偏移: "
              + snpc.getYDelta() + ", 索引: " + snpc.getWalkerIndex());
          }
        } else if (npc.isRandomWalker()) {
          sendInfo(admin, "\t随机移动范围: " + npc.getSpawn().getRandomWalkRange() + "米");
        }
      }
      sendInfo(admin, createZoneInfo(creature));
      sendInfo(admin, "[种族]\n\t种族: " + creature.getRace() + ", 部族: " + creature.getTribe() + ", 基础部族: " + creature.getBaseTribe());
      sendInfo(admin, "[您的关系]\n\t是敌人: " + admin.isEnemy(creature) + ", 可攻击: " + PlayerRestrictions.canAttack(admin, target));
      sendInfo(admin, "[目标的关系]\n\t是敌人: " + creature.isEnemy(admin)
        + (creature instanceof Npc ? ", 敌意: " + ((Npc) creature).getType(admin) : ""));
      sendInfo(admin, "[生命属性]\n\tHP: " + creature.getLifeStats().getCurrentHp() + " / " + creature.getLifeStats().getMaxHp()
          + "\n\tMP: " + creature.getLifeStats().getCurrentMp() + " / " + creature.getLifeStats().getMaxMp());
      sendInfo(admin, createAggroInfo(creature));
    } else if (target.getSpawn() != null && target.getSpawn().getStaticId() != 0) {
      sendInfo(admin, "\t静态ID: " + target.getSpawn().getStaticId());
    }
  }

  private String createZoneInfo(Creature creature) {
    FortressLocation fortress = SiegeService.getInstance().findFortress(creature.getWorldId(), creature.getX(), creature.getY(), creature.getZ());
    int townId = TownService.getInstance().getTownIdByPosition(creature);
    StringBuilder sb = new StringBuilder("[当前区域]");
    sb.append("\n\t" + creature.getPosition().toCoordString());
    sb.append("\n\t要塞位置ID: " + (fortress == null ? "-" : fortress.getLocationId()));
    sb.append("\n\t城镇ID: " + (townId == 0 ? "-" : townId));
    sb.append("\n\tPvP区域: " + creature.isInsidePvPZone());
    return sb.toString();
  }

  private String createAggroInfo(Creature creature) {
    StringBuilder sb = new StringBuilder("[仇恨列表]");
    int aDmg = 0, eDmg = 0, tDmg = creature.getAggroList().getTotalDamage();
    for (AggroInfo ai : creature.getAggroList().getList()) {
      String name = ai.getAttacker().getName();
      if (ai.getAttacker() instanceof Creature attacker) {
        Creature master = attacker.getMaster();
        if (master.getRace() == Race.ASMODIANS)
          aDmg += ai.getDamage();
        else if (master.getRace() == Race.ELYOS)
          eDmg += ai.getDamage();
        if (!master.equals(ai.getAttacker()))
          name = master.getName() + "的" + attacker.getObjectTemplate().getL10n();
      }
      sb.append("\n\t名称: " + name + ", 伤害: " + ai.getDamage() + ", 仇恨: " + ai.getHate());
    }
    if (tDmg > 0) {
      sb.append("\n\t总伤害: ").append(tDmg);
      sb.append("\n\t\t(魔族)伤害: ").append(aDmg);
      sb.append("\n\t\t(天族)伤害: ").append(eDmg);
      sb.append("\n\t\t(中立)伤害: ").append(tDmg - aDmg - eDmg);
    }
    return sb.toString();
  }
}