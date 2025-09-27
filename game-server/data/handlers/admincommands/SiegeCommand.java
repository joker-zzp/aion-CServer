package admincommands;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.siege.SiegeType;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.services.siege.BalaurAssaultService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 攻城战命令类 - 控制攻城战和神器相关操作
 */
public class SiegeCommand extends AdminCommand {

  public SiegeCommand() {
    super("siege", "控制攻城战和神器。");

    // @formatter:off
    setSyntaxInfo(
      "help - 显示攻城战命令帮助信息",
      "locations - 显示所有位置信息。",
      "start <locationId> - 在指定位置开始攻城战。",
      "stop <locationId> - 在指定位置停止攻城战。",
      "capture <locationId> [elyos|asmodians|balaur|军团名称|军团ID] - 占领指定位置的要塞。",
      "assault <locationId> [延迟秒数] - 在指定位置开始进攻。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player player, String... params) {
    switch (params.length == 0 ? "" : params[0].toLowerCase()) {
      case "help" -> sendInfo(player);
      case "locations" -> listLocations(player);
      case "start" -> startSiege(player, parseLocation(params));
      case "stop" -> stopSiege(player, parseLocation(params));
      case "capture" -> capture(player, parseLocation(params), params);
      case "assault" -> assault(player, parseLocation(params), params.length < 3 ? 0 : Integer.parseInt(params[2]));
      default -> sendInfo(player);
    }
  }

  private void listLocations(Player player) {
    Collection<List<SiegeLocation>> locations = SiegeService.getInstance().getSiegeLocations().values().stream()
      .sorted(Comparator.comparingInt(SiegeLocation::getLocationId))
      .collect(Collectors.groupingBy(l -> (l.getLocationId() - 1) / 10, LinkedHashMap::new, Collectors.toList())).values();
    for (List<SiegeLocation> siegeLocations : locations) {
      for (int i = 0; i < siegeLocations.size(); i++) {
        SiegeLocation loc = siegeLocations.get(i);
        String worldName = DataManager.WORLD_MAPS_DATA.getTemplate(loc.getTemplate().getWorldId()).getName();
        String name = loc.getTemplate().getL10nId() == 0 ? loc.getType().toString() : loc.getTemplate().getL10n();
        String message = name + " (ID: " + loc.getLocationId() + ") 在 " + worldName + " 属于 " + loc.getRace();
        int secondsLeft = SiegeService.getInstance().getRemainingSiegeTimeInSeconds(loc.getLocationId());
        if (secondsLeft > 0)
          message += " (距离攻城战结束还剩 " + secondsLeft / 60 + "分 " + secondsLeft % 60 + "秒)";
        if (i > 0 && loc.getType() == SiegeType.ARTIFACT)
          message = '\t' + message;
        sendInfo(player, message);
      }
    }
  }

  private void startSiege(Player player, SiegeLocation loc) {
    if (SiegeService.getInstance().isSiegeInProgress(loc.getLocationId())) {
      sendInfo(player, "该位置已经在攻城战中。");
    } else {
      SiegeService.getInstance().startSiege(loc.getLocationId());
      sendInfo(player, "已在" + getLocationName(loc) + "开始攻城战。");
    }
  }

  private void stopSiege(Player player, SiegeLocation loc) {
    if (!SiegeService.getInstance().isSiegeInProgress(loc.getLocationId())) {
      sendInfo(player, "该位置不在攻城战中。");
    } else {
      SiegeService.getInstance().stopSiege(loc.getLocationId());
      sendInfo(player, "已停止" + getLocationName(loc) + "的攻城战。");
    }
  }

  private void capture(Player player, SiegeLocation loc, String[] params) {
    SiegeRace sr = null;
    Legion legion = null;
    if (params.length >= 3) {
      try {
        sr = SiegeRace.valueOf(params[2].toUpperCase());
      } catch (IllegalArgumentException ignored) {
        try {
          int legionId = Integer.parseInt(params[2]);
          legion = LegionService.getInstance().getLegion(legionId);
        } catch (NumberFormatException e) {
          String legionName = "";
          for (int i = 2; i < params.length; i++)
            legionName += " " + params[i];
          legion = LegionService.getInstance().getLegion(legionName.trim());
        }
        if (legion != null) {
          sr = SiegeRace.getByRace(PlayerService.getOrLoadPlayerCommonData(legion.getBrigadeGeneral()).getRace());
        }
      }
      if (legion == null && sr == null) {
        sendInfo(player, params[2] + "不是有效的种族或军团");
        return;
      }
    } else {
      sr = SiegeRace.getByRace(player.getRace());
    }
    SiegeService.getInstance().captureSiege(sr, legion != null ? legion.getLegionId() : 0, loc.getLocationId());
  }

  private void assault(Player player, SiegeLocation loc, int delaySeconds) {
    if (BalaurAssaultService.getInstance().startAssault(loc.getLocationId(), delaySeconds))
      sendInfo(player, "已开始进攻" + getLocationName(loc));
    else {
      if (SiegeService.getInstance().isSiegeInProgress(loc.getLocationId()))
        sendInfo(player, getLocationName(loc) + "的进攻已经开始。");
      else
        sendInfo(player, "要开始进攻，" + getLocationName(loc) + "必须处于攻城战状态。");
    }
  }

  private SiegeLocation parseLocation(String[] params) {
    SiegeLocation location = params.length < 2 ? null : SiegeService.getInstance().getSiegeLocation(Integer.parseInt(params[1]));
    if (location == null)
      throw new IllegalArgumentException("无效的locationId。");
    return location;
  }

  private static Object getLocationName(SiegeLocation loc) {
    return loc.getTemplate().getL10nId() == 0 ? loc.getType().toString() + " " + loc.getLocationId() : loc.getTemplate().getL10n();
  }
}