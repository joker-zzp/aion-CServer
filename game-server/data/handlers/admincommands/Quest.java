package admincommands;

import java.util.Arrays;
import java.util.List;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.ENpcFactionQuestState;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.FinishedQuestCond;
import com.aionemu.gameserver.model.templates.quest.XMLStartCondition;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION.ActionType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_COMPLETED_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author MrPoke, Neon, Pad
 */
public class Quest extends AdminCommand {

	public Quest() {
		super("quest", "处理目标的任务状态。");

		// @formatter:off
		setSyntaxInfo(
			"[玩家] <任务ID> <reset|start|delete> - 重置/开始/删除指定的任务。",
			"[玩家] <任务ID> <status> - 显示指定任务的状态。",
			"[玩家] <任务ID> <set> <状态> <变量值> [变量索引] - 设置指定任务的状态（默认：将变量值应用于所有变量索引，可选：将变量值设置到特定变量索引[0-5]）。",
			"[玩家] <任务ID> <setflags> <标志值> - 设置指定任务的标志。",
			"[玩家] <任务ID> <dialog> <对话框ID> - 发送带有指定ID的对话框页面。",
			"注意：如果未指定玩家参数，则会使用当前目标（如果未选中玩家，则默认为您自己的角色）。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		byte index = 0;
		Player target;
		int questId = ChatUtil.getQuestId(params[index]);
		if (questId == 0) {
			target = World.getInstance().getPlayer(Util.convertName(params[index]));

			if (target == null || !target.isOnline()) {
				PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_MSG_ASK_PCINFO_LOGOFF());
				return;
			}

			if (++index >= params.length) {
				sendInfo(admin);
				return;
			}

			questId = ChatUtil.getQuestId(params[index]);
		} else {
			target = admin.getTarget() instanceof Player p ? p : admin;
		}

		if (questId == 0 || DataManager.QUEST_DATA.getQuestById(questId) == null) {
			sendInfo(admin, "无效的任务。");
			return;
		}

		if (++index >= params.length) {
			sendInfo(admin);
			return;
		}

		// quest and target are both valid at this point
		if (params[index].equalsIgnoreCase("reset")) {
			resetQuest(admin, target, questId);
		} else if (params[index].equalsIgnoreCase("start")) {
			startQuest(admin, target, questId);
		} else if (params[index].equalsIgnoreCase("delete")) {
			deleteQuest(admin, target, questId);
		} else if (params[index].equalsIgnoreCase("status")) {
			showQuestStatus(admin, target, questId);
		} else if (params[index].equalsIgnoreCase("set")) {
			QuestStatus status;
			int var;
			int varNum = -1;

			try {
				status = QuestStatus.valueOf(params[++index].toUpperCase());
			} catch (IllegalArgumentException e) {
				sendInfo(admin, "<状态> 必须是 " + Arrays.toString(QuestStatus.values()));
				return;
			} catch (IndexOutOfBoundsException e) {
				sendInfo(admin);
				return;
			}

			try {
				var = Integer.valueOf(params[++index]);
			} catch (NumberFormatException e) {
				sendInfo(admin, "<变量值> 必须是整数。");
				return;
			} catch (IndexOutOfBoundsException e) {
				sendInfo(admin);
				return;
			}

			if (++index < params.length) { // optional
				try {
					varNum = Integer.valueOf(params[index]);
					if (varNum < 0 || varNum > 5)
						throw new IllegalArgumentException();
				} catch (IllegalArgumentException e) { // also catches NumberFormatException
					sendInfo(admin, "[变量索引] 必须是0到5之间的整数。");
					return;
				}
			}

			setQuestStatus(admin, target, questId, status, var, varNum);
		} else if (params[index].equalsIgnoreCase("setflags")) {
			int flags;

			try {
				flags = Integer.valueOf(params[++index]);
			} catch (IndexOutOfBoundsException | NumberFormatException e) {
				sendInfo(admin, "<标志值> 必须是整数。");
				return;
			}

			setQuestFlags(admin, target, questId, flags);
		} else if (params[index].equalsIgnoreCase("dialog")) {
			int dialogPageId;

			try {
				dialogPageId = Integer.valueOf(params[++index]);
			} catch (IndexOutOfBoundsException | NumberFormatException e) {
				sendInfo(admin, "<对话框ID> 必须是整数。");
				return;
			}

			sendQuestDialog(admin, questId, dialogPageId);
		} else {
			sendInfo(admin);
		}
	}

	private void resetQuest(Player admin, Player target, int questId) {
		QuestState qs = target.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			sendInfo(admin, "只有当前进行中的任务才能重置。");
			return;
		}
		if (qs.getQuestVars().getQuestVars() == 0 && qs.getRewardGroup() == null) {
			sendInfo(admin, "玩家 " + target.getName() + "的任务已经在初始阶段。");
			return;
		}
		qs.setStatus(QuestStatus.START);
		qs.setQuestVar(0);
		qs.setRewardGroup(null);
		PacketSendUtility.sendPacket(target, new SM_QUEST_ACTION(ActionType.UPDATE, qs));
		sendInfo(admin, "已重置玩家 " + target.getName() + "的任务 " + ChatUtil.quest(questId) + "。");
	}

	private void startQuest(Player admin, Player target, int questId) {
		QuestTemplate template = DataManager.QUEST_DATA.getQuestById(questId);
		if (template.getNpcFactionId() > 0) {
			startNpcFactionQuest(admin, target, questId, template.getNpcFactionId());
			return;
		} else if (QuestService.startQuest(new QuestEnv(null, target, questId))) {
			sendInfo(admin, "已为玩家 " + target.getName() + "开始任务 " + ChatUtil.quest(questId) + "。");
			return;
		}
		QuestState qs = target.getQuestStateList().getQuestState(questId);
		if (qs != null && (qs.getStatus() == QuestStatus.START || qs.getStatus() == QuestStatus.REWARD)) {
			sendInfo(admin, "任务已经开始。");
		} else if (qs != null && qs.getStatus() == QuestStatus.COMPLETE && !qs.canRepeat()) {
			sendInfo(admin, "任务已经完成。");
		} else {
			StringBuilder sb = new StringBuilder();
			List<XMLStartCondition> preconditions = template.getXMLStartConditions();
			if (preconditions != null) {
				for (XMLStartCondition condition : preconditions) {
					List<FinishedQuestCond> finisheds = condition.getFinishedPreconditions();
					if (finisheds != null) {
						for (FinishedQuestCond fcondition : finisheds) {
							QuestState qs1 = target.getQuestStateList().getQuestState(fcondition.getQuestId());
							if (qs1 == null || qs1.getStatus() != QuestStatus.COMPLETE) {
								sb.append("\n\t" + ChatUtil.quest(fcondition.getQuestId()));
							}
						}
					}
				}
			}
			sendInfo(admin,
				"任务未开始。 " + (sb.length() > 0 ? "必须先完成以下任务：" + sb.toString() : "某些前置条件未满足。"));
		}
	}

	private void startNpcFactionQuest(Player admin, Player target, int questId, int factionId) {
		NpcFaction faction = target.getNpcFactions().getActiveNpcFaction(false);
		if (faction == null || faction.getId() != factionId) {
			sendInfo(admin, "玩家 " + target.getName() + " 未注册到该任务的组织中。");
			return;
		}
		for (QuestTemplate template : DataManager.QUEST_DATA.getQuestsByNpcFaction(faction.getId(), target)) {
			if (template.getId() == questId) {
				// simulate daily reset
				faction.setActive(false);
				faction.setTime(-1);
				target.getNpcFactions().addNpcFaction(faction);
				faction.setActive(true);
				// set daily quest Id and time to avoid random quest
				faction.setState(ENpcFactionQuestState.NOTING);
				faction.setQuestId(questId);
				faction.setTime(faction.getTime() + 100);
				// send the daily quest to player
				target.getNpcFactions().sendDailyQuest();
				sendInfo(admin, "已为玩家 " + target.getName() + "开始NPC组织任务 " + ChatUtil.quest(questId) + "。");
				return;
			}
		}
		sendInfo(admin, "任务未实现或玩家等级不匹配。");
	}

	private void deleteQuest(Player admin, Player target, int questId) {
		if (!admin.hasAccess(AdminConfig.CMD_QUEST_ADV_PARAMS)) {
			sendInfo(admin, "<使用此功能需要访问级别 " + AdminConfig.CMD_QUEST_ADV_PARAMS + " 或更高>");
			return;
		}
		QuestState qs = target.getQuestStateList().deleteQuest(questId);
		if (qs == null) {
			sendInfo(admin, "玩家 " + target.getName() + " 没有该任务。");
			return;
		}
		if (qs.getStatus() == QuestStatus.COMPLETE)
			QuestEngine.getInstance().sendCompletedQuests(target); // rewrite completed quest list
		else
			PacketSendUtility.sendPacket(target, new SM_QUEST_ACTION(ActionType.ABANDON, qs));
		target.getController().updateNearbyQuests();
		sendInfo(admin, "已删除玩家 " + target.getName() + "的任务 " + ChatUtil.quest(questId) + "。");
	}

	private void showQuestStatus(Player admin, Player target, int questId) {
		if (!admin.hasAccess(AdminConfig.CMD_QUEST_ADV_PARAMS)) {
			sendInfo(admin, "<使用此功能需要访问级别 " + AdminConfig.CMD_QUEST_ADV_PARAMS + " 或更高>");
			return;
		}
		QuestState qs = target.getQuestStateList().getQuestState(questId);
		StringBuilder sb = new StringBuilder("玩家: " + target.getName() + ", 任务: " + ChatUtil.quest(questId) + "\n\t任务状态: ");
		if (qs == null) {
			sb.append("NULL");
		} else {
			sb.append(qs.getStatus().toString());
			sb.append("\n\t任务变量:");
			for (int i = 0; i <= 5; i++)
				sb.append(" " + qs.getQuestVarById(i));
			sb.append(", 编码值 [" + qs.getQuestVars().getQuestVars() + "]");
			sb.append("\n\t任务标志: " + (qs.getFlags() & 0x3F) + " " + qs.getStepGroup()); // needs rework when flags are implemented like vars
			sb.append(", 编码值 [" + qs.getFlags() + "]");
		}
		sendInfo(admin, sb.toString());
	}

	private void setQuestStatus(Player admin, Player target, int questId, QuestStatus status, int var, int varNum) {
		if (!admin.hasAccess(AdminConfig.CMD_QUEST_ADV_PARAMS)) {
			sendInfo(admin, "<使用此功能需要访问级别 " + AdminConfig.CMD_QUEST_ADV_PARAMS + " 或更高>");
			return;
		}
		QuestState qs = target.getQuestStateList().getQuestState(questId);
		ActionType actionType;
		if (qs == null) { // player doesn't have that quest
			actionType = ActionType.ADD;
			qs = new QuestState(questId, status);
			target.getQuestStateList().addQuest(questId, qs);
		} else {
			actionType = qs.getStatus() == QuestStatus.COMPLETE ? ActionType.ADD : ActionType.UPDATE;
			qs.setStatus(status);
		}
		if (status == QuestStatus.COMPLETE) {
			qs.setQuestVar(0); // completed quests vars are always 0
			if (!DataManager.QUEST_DATA.getQuestById(qs.getQuestId()).getRewards().isEmpty())
				qs.setRewardGroup(0); // follow quests could require reward group > 0 to be unlocked (see quest_data.xml)
			QuestEngine.getInstance().onQuestCompleted(target, questId);
		} else {
			if (varNum == -1)
				qs.setQuestVar(var);
			else
				qs.setQuestVarById(varNum, var);
		}
		if (actionType == ActionType.ADD && status == QuestStatus.COMPLETE)
			PacketSendUtility.sendPacket(target, new SM_QUEST_COMPLETED_LIST(1, Arrays.asList(qs)));
		else
			PacketSendUtility.sendPacket(target, new SM_QUEST_ACTION(actionType, qs));
		target.getController().updateNearbyQuests();
		sendInfo(admin, "已设置玩家 " + target.getName() + "的任务 " + ChatUtil.quest(questId) + " 状态。");
	}

	private void setQuestFlags(Player admin, Player target, int questId, int flags) { // needs rework when flags are implemented like vars
		if (!admin.hasAccess(AdminConfig.CMD_QUEST_ADV_PARAMS)) {
			sendInfo(admin, "<使用此功能需要访问级别 " + AdminConfig.CMD_QUEST_ADV_PARAMS + " 或更高>");
			return;
		}
		QuestState qs = target.getQuestStateList().getQuestState(questId);
		if (qs == null || qs.getStatus() != QuestStatus.START) {
			sendInfo(admin, "标志只能设置给进行中的任务。");
			return;
		}
		qs.setFlags(flags);
		PacketSendUtility.sendPacket(target, new SM_QUEST_ACTION(ActionType.UPDATE, qs));
		sendInfo(admin, "已将玩家 " + target.getName() + "的任务标志设置为 " + flags + "。");
	}

	private void sendQuestDialog(Player admin, int questId, int dialogPageId) {
		if (!admin.hasAccess(AdminConfig.CMD_QUEST_ADV_PARAMS)) {
			sendInfo(admin, "<使用此功能需要访问级别 " + AdminConfig.CMD_QUEST_ADV_PARAMS + " 或更高>");
			return;
		}
		PacketSendUtility.sendPacket(admin, new SM_DIALOG_WINDOW(0, dialogPageId, questId));
		sendInfo(admin, "已发送任务 Q" + questId + " 的对话框页面 " + dialogPageId + "。");
	}
}