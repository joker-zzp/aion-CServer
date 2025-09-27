package admincommands;

import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.aionemu.commons.network.NioServer;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Neon
 */
public class Debug extends AdminCommand {

  public Debug() {
    super("debug", "帮助解决运行时问题");

    // @formatter:off
    setSyntaxInfo(
      "<connections> - 显示所有已连接的游戏客户端",
      "<connectedPlayers> - 显示已连接玩家的信息",
      "<dcBuggedPlayers> - 断开并尝试保存有问题的玩家"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    if ("connections".equalsIgnoreCase(params[0])) {
      List<AionConnection> connections = findAionConnections(admin);
      if (connections != null) {
        sendInfo(admin, "在线客户端:\n\t" + connections.stream().map(AionConnection::toString).collect(Collectors.joining("\n\t")));
      }
    } else if ("connectedPlayers".equalsIgnoreCase(params[0])) {
      List<Player> connectedPlayers = findConnectedPlayers(admin);
      if (connectedPlayers != null) {
        String message = "已连接玩家 (" + connectedPlayers.size() + "):";
        for (Player player : connectedPlayers) {
          String details = "位置: " + player.getPosition().toCoordString() + ", 已生成: " + player.isSpawned();
          if (!player.isInWorld()) {
            details += ", " + ChatUtil.color("不在世界中", Color.RED);
          }
          message += "\n\t" + player.getName() + " [" + details + "]";
        }
        sendInfo(admin, message);
      }
    } else if ("dcBuggedPlayers".equalsIgnoreCase(params[0])) {
      List<Player> buggedPlayers = findConnectedPlayers(admin).stream().filter(p -> !p.isInWorld()).collect(Collectors.toList());
      if (buggedPlayers != null) {
        if (buggedPlayers.isEmpty()) {
          sendInfo(admin, "未找到有问题的玩家");
        } else {
          for (Player player : buggedPlayers) {
            player.getController().cancelAllTasks(); // ensure to cancel item update task etc
            player.getCommonData().setOnline(false);
            PlayerService.storePlayer(player);
            player.getClientConnection().setActivePlayer(null);
            player.getClientConnection().close();
            player.setClientConnection(null);
          }
          sendInfo(admin, "已保存大部分数据并断开以下玩家的连接:\n" + buggedPlayers);
        }
      }
    }
  }

  private List<Player> findConnectedPlayers(Player admin) {
    List<AionConnection> connections = findAionConnections(admin);
    if (connections != null) {
      return connections.stream().map(AionConnection::getActivePlayer).filter(Objects::nonNull)
        .sorted(Comparator.comparing(Player::getName)).collect(Collectors.toList());
    }
    return null;
  }

  private List<AionConnection> findAionConnections(Player admin) {
    try {
      Field nioServerField = GameServer.class.getDeclaredField("nioServer");
      boolean oldAccessible = nioServerField.isAccessible();
      nioServerField.setAccessible(true);
      NioServer nioServer = (NioServer) nioServerField.get(null);
      nioServerField.setAccessible(oldAccessible);
      java.util.Set<SelectionKey> keys = nioServer.getReadWriteDispatcher().selector().keys();
      return keys.stream().map(SelectionKey::attachment).filter(o -> o instanceof AionConnection).map(o -> (AionConnection) o)
        .collect(Collectors.toList());
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
      sendInfo(admin, e.toString());
      return null;
    }
  }

}