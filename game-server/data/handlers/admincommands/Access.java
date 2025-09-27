package admincommands;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.services.CommandsAccessService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author ViAl, Neon
 */
public class Access extends AdminCommand {

  private final Map<Integer, Byte> oldAccessLevels = new HashMap<>();

  public Access() {
    super("access", "聊天命令和访问级别管理");

    // @formatter:off
    setSyntaxInfo(
      "<add> <player name> <command name> - 授予玩家对给定聊天命令的访问权限。",
      "<remove> <player name> <command name> - 从玩家移除对给定聊天命令的访问权限。",
      "<removeall> <player name> - 从玩家移除所有授予的访问权限。",
      "<level> <number> - 临时将玩家访问级别设置为给定的较低值（仅用于测试）。",
      "<level> <reset> - 重置玩家访问级别为原始值。",
      "<set> <player name> <access level> - 设置玩家帐号的访问级别。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length <= 1) {
      sendInfo(admin);
      return;
    }
    String cmd = params[0].toLowerCase();
    switch (cmd) {
      case "add":
      case "remove": {
        if (params.length < 3) {
          sendInfo(admin);
          return;
        }
        String playerName = Util.convertName(params[1]);
        String commandName = params[2];
        PlayerCommonData pcd = PlayerService.getOrLoadPlayerCommonData(playerName);
        if (pcd == null) {
          sendInfo(admin, "玩家" + playerName + "不存在");
          return;
        }
        if (cmd.equals("add"))
          CommandsAccessService.giveAccess(admin, pcd.getPlayerObjId(), commandName);
        else
          CommandsAccessService.removeAccess(admin, pcd.getPlayerObjId(), commandName);
        break;
      }
      case "removeall": {
        String playerName = Util.convertName(params[1]);
        PlayerCommonData pcd = PlayerService.getOrLoadPlayerCommonData(playerName);
        if (pcd == null) {
          sendInfo(admin, "玩家" + playerName + "不存在");
          return;
        }
        if (CommandsAccessService.removeAllAccesses(pcd.getPlayerObjId()))
          sendInfo(admin, "已移除玩家" + pcd.getName() + "的所有自定义聊天命令访问权限");
        else
          sendInfo(admin, "没有可从玩家" + pcd.getName() + "移除的内容");
        break;
      }
      case "level": {
        byte currentLevel = admin.getAccount().getAccessLevel();
        byte maxLevel = oldAccessLevels.getOrDefault(admin.getObjectId(), currentLevel);
        if ("reset".equalsIgnoreCase(params[1])) {
          if (maxLevel == currentLevel) {
            sendInfo(admin, "无需重置");
            return;
          }
          admin.getAccount().setAccessLevel(maxLevel);
          sendInfo(admin, "您的访问级别已重置");
        } else {
          byte level = NumberUtils.toByte(params[1], (byte) -1);
          if (level == -1 || level > maxLevel) {
            sendInfo(admin, "无效的访问级别");
            return;
          }
          if (level == currentLevel) {
            sendInfo(admin, "您已经拥有访问级别" + level);
            return;
          }
          admin.getAccount().setAccessLevel(level);
          if (level < getLevel() && !CommandsAccessService.hasAccess(admin.getObjectId(), getAlias())) {
            CommandsAccessService.giveTemporaryAccess(admin, admin.getObjectId(), getAlias());
          }
          sendInfo(admin, "您的访问级别已更改");
        }
        currentLevel = admin.getAccount().getAccessLevel();
        if (currentLevel == maxLevel) {
          oldAccessLevels.remove(admin.getObjectId());
        } else {
          oldAccessLevels.putIfAbsent(admin.getObjectId(), maxLevel);
        }
        if (currentLevel > getLevel() && CommandsAccessService.hasAccess(admin.getObjectId(), getAlias())) {
          CommandsAccessService.removeAccess(admin, admin.getObjectId(), getAlias());
        }
        admin.getController().onChangedPlayerAttributes();
        break;
      }
      case "set": {
        // 设置玩家帐号的访问级别
        if (params.length < 3) {
          sendInfo(admin, "语法错误");
          return;
        }
        // 获取玩家帐号
        String playerName = Util.convertName(params[1]);
        // 获取访问级别
        byte accessLevel = NumberUtils.toByte(params[2], (byte) -1);
        if (accessLevel == -1 || accessLevel > 10) {
          sendInfo(admin, "无效的等级, 请设置[0 - 9]的值");
          return;
        }
        // 加载玩家数据
        PlayerCommonData pcd = PlayerService.getOrLoadPlayerCommonData(playerName);
        if (pcd == null) {
          sendInfo(admin, "玩家" + playerName + "不在线");
          return;
        }
        // 检查玩家是否在线
        Player player = pcd.getPlayer();
        if (player == null) {
          sendInfo(admin, "玩家" + playerName + "不存在");
          return;
        }
        // 获取账号对象
        Account account = player.getAccount();
        // 设置账号的访问级别
        account.setAccessLevel(accessLevel);
        // 同步账号数据
        LoginServer.getInstance().sendLsControlPacket(1, accessLevel, player, admin);
        // 同步
        // 发送成功消息
        sendInfo(admin, "玩家" + playerName + "的帐号" + account.getName() + "访问级别已设置为" + accessLevel);
        break;
      }
      default:
        sendInfo(admin);
    }
  }

}