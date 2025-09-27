package admincommands;

import java.util.Collection;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.group.PlayerGroup;
import com.aionemu.gameserver.model.team.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员队伍命令, 用于创建单人队伍和查看队伍信息
 * 
 * @author joekr-zzp
 */
public class Group extends AdminCommand {

  public Group() {
    super("group", "管理队伍功能");

    // @formatter:off
    setSyntaxInfo(
      "- create 创建单人队伍",
      "- info 查看队伍信息"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    // 检查参数长度
    if (params.length < 1) {
      sendInfo(admin);
      return;
    }

    String command = params[0].toLowerCase();

    // 创建单人队伍
    if ("create".equals(command)) {
      handleCreateGroup(admin);
    } else if ("info".equals(command)) {
      handleInfoGroup(admin);
    }
  }

  /**
   * 处理创建单人队伍的逻辑
   */
  private void handleCreateGroup(Player player) {
    // 检查玩家是否已在队伍中
    if (player.getCurrentTeam() instanceof PlayerGroup) {
      sendInfo(player, "您已经在队伍中，无法创建新的队伍。");
      return;
    }

    try {
      // 创建单人队伍
      PlayerGroupService.createSoloGroup(player);
      sendInfo(player, "已成功创建单人队伍。");
      // 替换不存在的系统消息方法为正确的方法
      PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_YOU_BECOME_NEW_LEADER());
    } catch (Exception e) {
      sendInfo(player, "创建单人队伍时出错：" + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * 发送队伍信息
   */
  private void handleInfoGroup(Player player) {
    Player targetPlayer = player;
    // 修正逻辑判断，检查是否不在队伍中
    if (!(targetPlayer.getCurrentTeam() instanceof PlayerGroup)) {
      sendInfo(player,
        targetPlayer.equals(player) ?
          "您不在队伍中." :
          targetPlayer.getName() + "当前不在任何队伍中."
      );
      return;
    }
    PlayerGroup group = (PlayerGroup) targetPlayer.getCurrentTeam();

    // 构建队伍信息
    StringBuilder info = new StringBuilder();
    info.append("[队伍信息]");
    info.append("\n\t 队长: " + group.getLeader().getName());
    info.append("\n\t 队伍类型: " + group.getTeamType());
    info.append("\n\t 成员数量: " + group.size() + "/" + group.getMaxMemberCount());

      // 发送信息
    sendInfo(player, info.toString());
  }
}