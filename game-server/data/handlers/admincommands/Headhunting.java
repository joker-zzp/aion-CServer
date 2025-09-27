package admincommands;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.dao.HeadhuntingDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.event.Headhunter;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.templates.rewards.RewardItem;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.PvpService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 处理季节性猎头活动的分析和奖励的命令。
 * 使用reward参数时请小心，它会清除所有引用，包括数据库表。
 * 
 * @author Estrayl
 */
public class Headhunting extends AdminCommand {

  private static final Logger log = LoggerFactory.getLogger("EVENT_LOG");

  /**
   * Contains the rewards for different rankings (key value).
   */
  private final Map<Integer, List<RewardItem>> rewards = new LinkedHashMap<>();
  private final List<RewardItem> consolationRewards = new ArrayList<>();

  /**
   * Contains a sorted descending list of {@link Headhunter} by kills per {@link PlayerClass}.
   */
  private Map<Race, Map<PlayerClass, List<Headhunter>>> results;

  public Headhunting() {
    super("headhunting");

    // @formatter:off
    setSyntaxInfo(
      "<analyze> - 分析当前赛季。",
      "<show> <rewards|results> - 显示已注册的奖励或分析结果",
      "<clear> - 清除分析结果",
      "<addKills> <playerId> <kills> - 为指定玩家添加猎头击杀数。",
      "<finalize> <true|false> - 结束赛季（清除所有引用）并根据请求奖励所有参与者"
    );
    // @formatter:on

    // Initialize seasonal headhunting rewards
    rewards.put(1, new ArrayList<>());
    rewards.put(2, new ArrayList<>());
    rewards.put(3, new ArrayList<>());

    rewards.get(1).add(new RewardItem(164002276, 5)); // Eternal War Battle Scroll
    rewards.get(1).add(new RewardItem(188950017, 3)); // Special Courier Pass (Abyss Eternal/Lv. 61-65)
    rewards.get(1).add(new RewardItem(186000051, 15)); // Major Ancient Crown

    rewards.get(2).add(new RewardItem(164002276, 3)); // Eternal War Battle Scroll
    rewards.get(2).add(new RewardItem(188950017, 3)); // Special Courier Pass (Abyss Eternal/Lv. 61-65)
    rewards.get(2).add(new RewardItem(186000051, 10)); // Major Ancient Crown

    rewards.get(3).add(new RewardItem(164002276, 1)); // Eternal War Battle Scroll
    rewards.get(3).add(new RewardItem(188950017, 2)); // Special Courier Pass (Abyss Eternal/Lv. 61-65)
    rewards.get(3).add(new RewardItem(186000051, 5)); // Major Ancient Crown

    consolationRewards.add(new RewardItem(186000051, 1)); // Major Ancient Crown
  }

  @Override
  protected void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }
    switch (params[0]) {
      case "analyze":
        if (analyzeSeason(admin))
          sendInfo(admin, "赛季分析成功。");
        break;
      case "clear":
        if (results != null) {
          results.clear();
          results = null;
          sendInfo(admin, "结果清除成功。");
        }
        break;
      case "show":
        if (params.length == 1)
          sendInfo(admin);
        else if (params[1].equalsIgnoreCase("rewards"))
          showRewards(admin);
        else if (params[1].equalsIgnoreCase("results"))
          showResults(admin);
        break;
      case "addKills":
        if (params.length < 3) {
          sendInfo(admin);
          return;
        }
        try {
          addKills(admin, Integer.parseInt(params[1]), Integer.parseInt(params[2]));
        } catch (NumberFormatException e) {
          sendInfo(admin, "playerId和kills应为数字。");
        }
        break;
      case "finalize":
        if (params.length < 2) {
          sendInfo(admin);
          return;
        }
        boolean shouldReward = Boolean.parseBoolean(params[1]);
        finalizeSeason(admin, shouldReward);
        break;
      default:
        sendInfo(admin);
    }
  }

  private void addKills(Player admin, int playerId, int kills) {
    if (!EventsConfig.ENABLE_HEADHUNTING) {
      sendInfo(admin, "赛季未激活。");
      return;
    }
    Headhunter hunter = PvpService.getInstance().getHeadhunterById(playerId);
    if (hunter != null) {
      int newKills = hunter.getKills() + kills;
      if (newKills < 0)
        newKills = 0;

      hunter.setKills(newKills);
      hunter.setPersistentState(PersistentState.UPDATE_REQUIRED);
      sendInfo(admin, "成功为玩家添加" + kills + "次击杀 [ID=" + playerId + ", name=" + PlayerService.getPlayerName(playerId)
          + ", 当前击杀数=" + newKills + "].");
    } else {
      sendInfo(admin, "找不到该玩家。玩家ID错误。");
    }
  }

  private boolean analyzeSeason(Player admin) {
    if (results != null) {
      sendInfo(admin, "赛季仍在确认中。使用<show>参数获取最终信息概览, 或使用<reward>执行奖励流程.");
      return false;
    }
    final Map<Integer, Headhunter> headhunters = PvpService.getInstance().getAllHeadhunters();
    results = new EnumMap<>(Race.class);
    results.put(Race.ASMODIANS, new EnumMap<>(PlayerClass.class));
    results.put(Race.ELYOS, new EnumMap<>(PlayerClass.class));
    for (Headhunter hunter : headhunters.values()) {
      PlayerCommonData pcd = PlayerService.getOrLoadPlayerCommonData(hunter.getHunterId());
      if (pcd == null) {
        log.warn("PlayerID: " + hunter.getHunterId() + " did not exist anymore.");
        continue;
      }
      PlayerClass pc = pcd.getPlayerClass();
      Race race = pcd.getRace();
      results.get(race).putIfAbsent(pc, new ArrayList<>());
      List<Headhunter> hunterz = results.get(race).get(pc);
      hunterz.add(hunter);
      hunterz.sort(null);
    }
    return true;
  }

  private void showResults(Player admin) {
    if (results == null) {
      sendInfo(admin, "没有可用的结果！猎头赛季需要先分析再奖励。");
      return;
    } else if (results.isEmpty()) {
      results = null;
      sendInfo(admin, "尚未有玩家参与。已清除空奖励映射。");
      return;
    }
    StringBuilder builder = new StringBuilder();
    for (Race race : results.keySet()) {
      Map<PlayerClass, List<Headhunter>> raceMap = results.get(race);
      builder.append("<hr><center>").append(race).append("</center>");
      for (PlayerClass pc : raceMap.keySet()) {
        builder.append("<br><br>").append(pc.toString()).append("<br>");
        List<Headhunter> hunterz = raceMap.get(pc);
        for (int pos = 0; pos < hunterz.size(); pos++) {
          Headhunter hunter = hunterz.get(pos);
          if (pos + 1 > rewards.size() && hunter.getKills() < EventsConfig.HEADHUNTING_CONSOLATION_PRIZE_KILLS)
            break;

          String name = PlayerService.getPlayerName(hunter.getHunterId());
          builder.append("<br>").append(pos + 1).append(". ").append(name).append(" Kills: ").append(hunter.getKills());
        }
      }
    }
    HTMLService.showHTML(admin, HTMLCache.getInstance().getHTML("headhunting.xhtml").replace("HUNTERZ", builder));
  }

  private void showRewards(Player admin) {
    if (rewards.isEmpty()) {
      sendInfo(admin, "没有可用的奖励！");
      return;
    }
    StringBuilder builder = new StringBuilder();
    builder.append("<hr><center>Rewards</center><br><hr>");
    for (Integer rank : rewards.keySet()) {
      builder.append("<br><br><br>Rank: ").append(rank).append("<br>");
      List<RewardItem> items = rewards.get(rank);
      for (RewardItem item : items)
        builder.append("<br>").append(item.getCount()).append("x ").append(DataManager.ITEM_DATA.getItemTemplate(item.getId()).getName());
    }
    if (!consolationRewards.isEmpty()) {
      builder.append("<br><br>Consolation prize for >= ").append(EventsConfig.HEADHUNTING_CONSOLATION_PRIZE_KILLS).append(" kills<br>");
      for (RewardItem item : consolationRewards)
        builder.append("<br>").append(item.getCount()).append("x ").append(DataManager.ITEM_DATA.getItemTemplate(item.getId()).getName());
    }
    HTMLService.showHTML(admin, HTMLCache.getInstance().getHTML("headhunting.xhtml").replace("HUNTERZ", builder));
  }

  private void rewardPlayers(Player admin) {
    int rewardedPlayers = 0;
    int sentMails = 0;
    for (Race race : results.keySet()) {
      Map<PlayerClass, List<Headhunter>> raceMap = results.get(race);
      for (PlayerClass pc : raceMap.keySet()) {
        List<Headhunter> hunterz = raceMap.get(pc);
        for (int pos = 0; pos < hunterz.size(); pos++) {
          final Headhunter hunter = hunterz.get(pos);
          List<RewardItem> items = null;
          if (pos < 3)
            items = rewards.get(pos + 1);
          else if (hunter.getKills() >= EventsConfig.HEADHUNTING_CONSOLATION_PRIZE_KILLS)
            items = consolationRewards;

          if (items == null || items.isEmpty())
            continue;

          String name = PlayerService.getPlayerName(hunter.getHunterId());
          String rank;
          switch (pos) {
            case 0:
              rank = "1st";
              break;
            case 1:
              rank = "2nd";
              break;
            case 2:
              rank = "3rd";
              break;
            default:
              rank = "consolation";
          }
          for (RewardItem item : items) {
            if (SystemMailService.sendMail("Headhunting Corp", name, "Rewards",
              "恭喜您在本赛季以总计" + hunter.getKills() + "次击杀获得了" + rank + "名的成绩。", item.getId(),
              item.getCount(), 0, LetterType.BLACKCLOUD)) {
              sentMails++;
            } else {
              log.error("无法向玩家" + name + "发送奖励邮件，排名：" + rank + "。（物品ID：" + item.getId() + " 数量："
                + item.getCount() + "）。");
            }
          }
          log.info(
                "[种族：" + race + "] [职业：" + pc + "] [排名：" + rank + "] [奖励玩家：" + name + "] [击杀数：" + hunter.getKills() + "]");
          rewardedPlayers++;
        }
      }
    }
    sendInfo(admin, "成功奖励了" + rewardedPlayers + "名玩家并发送了" + sentMails + "封邮件。");
  }

  private void finalizeSeason(Player admin, boolean shouldReward) {
    if (EventsConfig.ENABLE_HEADHUNTING) {
      sendInfo(admin, "赛季仍在活跃中！您必须先禁用猎头活动才能奖励参与者。");
      return;
    } else if (shouldReward && results == null) {
      sendInfo(admin, "没有可用的结果！猎头赛季需要先分析再奖励。");
      return;
    } else if (shouldReward && results.isEmpty()) {
      results = null;
      sendInfo(admin, "本赛季无人参与。已清除空奖励映射。");
      return;
    }

    if (shouldReward)
      rewardPlayers(admin);

    PvpService.getInstance().finalizeHeadhuntingSeason();
    HeadhuntingDAO.clearTables();
    results.clear();
    results = null;
    sendInfo(admin, "成功清除了本赛季的所有引用并完成了归档。");
  }
}