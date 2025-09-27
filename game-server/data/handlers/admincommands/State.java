package admincommands;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 生物状态命令类 - 允许管理员查看和调整目标生物的各种状态
 * @author Rolandas
 */
public class State extends AdminCommand {

  public State() {
    super("state", "查看和调整目标的生物状态。");

    // @formatter:off
    setSyntaxInfo(
        "help - 显示状态命令的帮助信息",
        " - 显示目标生物的当前状态。",
        "<state> - 通过名称或ID设置给定的生物状态，替换现有状态。",
        "add <state> - 通过名称或ID添加给定的生物状态。",
        "remove <state> - 通过名称或ID移除给定的生物状态。使用-1移除所有状态。",
        "list - 显示所有可能的状态名称和ID。将ID值相加可以一次添加或移除多个状态。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    // 处理help参数
    if (params.length > 0 && "help".equalsIgnoreCase(params[0])) {
      sendInfo(admin);
      return;
    }
    
    VisibleObject target = admin.getTarget();
    if (target == null) {
      sendInfo(admin);
      return;
    }
    if (!(target instanceof Creature creature)) {
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
      return;
    }

    if (params.length == 0) {
      sendInfo(admin, creature.getName() + "的状态: " + getStateDescription(creature.getState()) + "\n更多选项请查看 "+ ChatUtil.color(getAliasWithPrefix() + " help", Color.WHITE));
    } else if ("list".equalsIgnoreCase(params[0])) {
      sendInfo(admin, "已知状态:\n\t" + Arrays.stream(CreatureState.values()).map(c -> c.name() + " (" + c.getId() + ')').collect(Collectors.joining("\n\t")));
    } else {
      int stateIndex = "add".equalsIgnoreCase(params[0]) || "remove".equalsIgnoreCase(params[0]) ? 1 : 0;
      if (params.length <= stateIndex) {
        sendInfo(admin, "Please provide a state name or ID.");
        return;
      }
      int stateId;
      try {
        stateId = CreatureState.valueOf(params[stateIndex].toUpperCase()).getId();
      } catch (IllegalArgumentException e) {
        stateId = Integer.parseInt(params[stateIndex]);
        if (stateId < 0 || stateId > 0xFFFF) {
          sendInfo(admin, "状态ID超出范围。");
          return;
        }
      }
      int newState;
      if (stateIndex == 0)
        newState = stateId & 0xFFFF;
      else if ("add".equalsIgnoreCase(params[0]))
        newState = (creature.getState() | stateId) & 0xFFFF;
      else
        newState = (creature.getState() & ~stateId) & 0xFFFF;

      creature.setState(newState);

      if (target instanceof Player player) {
        player.getController().onChangedPlayerAttributes();
      } else {
        creature.clearKnownlist();
        creature.updateKnownlist();
      }
      ThreadPoolManager.getInstance().schedule(() -> {
        admin.setTarget(target);
        PacketSendUtility.sendPacket(admin, new SM_TARGET_SELECTED(target));
        PacketSendUtility.broadcastToSightedPlayers(admin, new SM_TARGET_UPDATE(admin));
      }, 200);

      sendInfo(admin, creature.getName() + "的状态已更改为 " + getStateDescription(creature.getState()));
    }
  }

  private String getStateDescription(int state) {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= (state & 0xFFFF); i *= 2) {
      if ((state & i) == i) {
        if (!sb.isEmpty())
          sb.append(" + ");
        sb.append(findStateName(i, "UNK")).append(" (").append(i).append(')');
      }
    }
    return state + (sb.isEmpty() ? "" : " = " + sb.toString());
  }

  private String findStateName(int creatureStateId, String defaultName) {
    return Arrays.stream(CreatureState.values()).filter(s -> s.getId() == creatureStateId).findFirst().map(Object::toString).orElse(defaultName);
  }
}