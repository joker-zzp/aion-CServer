package admincommands;

import java.lang.reflect.Field;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.ai.*;
import com.aionemu.gameserver.ai.event.AIEventLog;
import com.aionemu.gameserver.ai.event.AIEventType;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.model.animations.ObjectDeleteAnimation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author ATracer, Neon
 */
public class Ai extends AdminCommand {

	public Ai() {
		super("ai", "修改和显示AI详细信息");

		// @formatter:off
		setSyntaxInfo(
			"<info> - 显示目标的AI信息",
			"<set> <ai名称> - 更改目标的AI",
			"<state> <状态名称> [子状态名称] - 更改AI状态",
			"<event> <事件名称> - 触发指定名称的AI事件",
			"<event2> <事件名称> <生物对象ID> - 为指定名称和生物触发AI事件",
			"<events> - 显示目标的最近AI事件",
			"<log> - 切换目标的AI日志记录开关",
			"<createlog|eventlog|movelog> - 切换日志记录开关",
			"<marker> [文本] - 在日志中打印带有可选文本的标记"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		if (params[0].equalsIgnoreCase("createlog")) {
			AIConfig.ONCREATE_DEBUG = !AIConfig.ONCREATE_DEBUG;
			sendInfo(admin, "新的createlog值: " + AIConfig.ONCREATE_DEBUG);
		} else if (params[0].equalsIgnoreCase("eventlog")) {
			AIConfig.EVENT_DEBUG = !AIConfig.EVENT_DEBUG;
			sendInfo(admin, "新的eventlog值: " + AIConfig.EVENT_DEBUG);
		} else if (params[0].equalsIgnoreCase("movelog")) {
			AIConfig.MOVE_DEBUG = !AIConfig.MOVE_DEBUG;
			sendInfo(admin, "新的movelog值: " + AIConfig.MOVE_DEBUG);
		} else if (params[0].equalsIgnoreCase("marker")) {
			if (params.length > 1)
				LoggerFactory.getLogger(AILogger.class).info("[AI] marker: " + StringUtils.join(params, ' ', 1, params.length));
			else
				LoggerFactory.getLogger(AILogger.class).info("[AI] marker");
		} else {
			VisibleObject target = admin.getTarget();
			if (target == null || !(target instanceof Creature)) {
				PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
				return;
			}
			Creature npc = (Creature) target;

			if (params[0].equalsIgnoreCase("info")) {
				sendInfo(admin,
						"[AI信息]\n\t名称: " + npc.getAi().getName() + "\n\t状态: " + npc.getAi().getState() + "\n\t子状态: " + npc.getAi().getSubState());
			} else if (params[0].equalsIgnoreCase("log")) {
				boolean oldValue = npc.getAi().isLogging();
				npc.getAi().setLogging(!oldValue);
				sendInfo(admin, "新的日志值: " + !oldValue);
			} else if (params[0].equalsIgnoreCase("events")) {
				AIEventLog eventLog = npc.getAi().getEventLog();
				if (eventLog == null || eventLog.isEmpty()) {
					sendInfo(admin, "没有记录的事件" + (AIConfig.EVENT_DEBUG ? "" : " (通过eventlog参数启用事件日志记录)"));
				} else {
					for (AIEventType eventType : eventLog) {
						sendInfo(admin, "事件: " + eventType.name());
					}
				}
			} else if (params.length > 1) {
				String param1 = params[1];
				if (params[0].equalsIgnoreCase("set")) {
					String aiName = param1;
					try {
						AI newAi = AIEngine.getInstance().newAI(aiName, npc);
						try {
							Field aiField = npc.getClass().getSuperclass().getDeclaredField("ai");
							aiField.setAccessible(true);
							World.getInstance().despawn(npc, ObjectDeleteAnimation.NONE);
							aiField.set(npc, newAi);
							World.getInstance().spawn(npc); // properly init AI states
						} catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
							LoggerFactory.getLogger(Ai.class).error("", e);
						}
						if (npc.getAi() == newAi)
							sendInfo(admin, "NPC现在拥有AI " + newAi.getClass().getSimpleName());
						else
							sendInfo(admin, "更改AI时出错(查看日志)");
					} catch (IllegalArgumentException e) {
						sendInfo(admin, e.getMessage());
					}
				} else if (params[0].equalsIgnoreCase("event")) {
					try {
						AIEventType eventType = AIEventType.valueOf(param1.toUpperCase());
						npc.getAi().onGeneralEvent(eventType);
					} catch (IllegalArgumentException e) {
						sendInfo(admin, "Found no event with that name");
					}
				} else if (params[0].equalsIgnoreCase("event2")) {
					Creature creature = params.length < 3 ? null : (Creature) World.getInstance().findVisibleObject(Integer.valueOf(params[2]));
					if (creature == null)
						sendInfo(admin, "请提供有效的生物对象ID");
					else {
						try {
							AIEventType eventType = AIEventType.valueOf(param1.toUpperCase());
							npc.getAi().onCreatureEvent(eventType, creature);
						} catch (IllegalArgumentException e) {
							sendInfo(admin, "未找到该名称的事件");
						}
					}
				} else if (params[0].equalsIgnoreCase("state")) {
					AIState state = null;
					try {
						state = AIState.valueOf(param1.toUpperCase());
						npc.getAi().setStateIfNot(state);
						if (params.length > 2) {
							AISubState substate = AISubState.valueOf(params[2]);
							npc.getAi().setSubStateIfNot(substate);
						}
					} catch (IllegalArgumentException e) {
						sendInfo(admin, "未找到该名称的" + (state == null ? "状态" : "子状态"));
					}
				}
			} else {
				sendInfo(admin);
			}
		}
	}

}