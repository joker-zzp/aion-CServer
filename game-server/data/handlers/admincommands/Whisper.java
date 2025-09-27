package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.CustomPlayerState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 私聊控制命令类 - 允许管理员启用或禁用接收私聊消息
 */
public class Whisper extends AdminCommand {

  public Whisper() {
    super("whisper", "启用/禁用接收私聊消息");

    setSyntaxInfo(
      "<help> - 显示私聊命令的帮助信息。",
      "<on|off> - 启用或禁用接收其他玩家的私聊消息（GM始终可以向你发送私聊）。"
    );
  }

  /**
   * 执行私聊控制命令
   * @param admin 执行命令的管理员玩家对象
   * @param params 命令参数数组，支持的参数：help、on、off
   */
  @Override
  public void execute(Player admin, String... params) {
    // 如果没有提供参数，显示帮助信息
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    // 处理不同的命令参数
    if (params[0].equalsIgnoreCase("help")) {
      // 显示命令帮助信息
      sendInfo(admin);
      return;
    } else if (params[0].equalsIgnoreCase("off")) {
      // 关闭接收私聊消息功能
      admin.setCustomState(CustomPlayerState.NO_WHISPERS_MODE);
      sendInfo(admin, "接收私聊消息: 关闭");
    } else if (params[0].equalsIgnoreCase("on")) {
      // 开启接收私聊消息功能
      admin.unsetCustomState(CustomPlayerState.NO_WHISPERS_MODE);
      sendInfo(admin, "接收私聊消息: 开启");
    }
  }
}