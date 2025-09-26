package admincommands;

import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.team.group.PlayerGroup;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author lyahim, antness
 */
public class PlayerInfo extends AdminCommand {

	public PlayerInfo() {
		super("playerinfo", "显示关于玩家的信息。");

		setSyntaxInfo("<玩家名称> <" + 
			"loc(位置)|" + 
			"item(物品)|" + 
			"group(队伍)|" + 
			"skills(技能)|" + 
			"legion(军团)|" + 
			"ap(深渊点数)|" + 
			"chars(角色)|" + 
			"known(已知列表)|" + 
			"visible(可见列表)>");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		String playerName = Util.convertName(params[0]);
		Player target = World.getInstance().getPlayer(playerName);
		if (target == null) {
			PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_NO_SUCH_USER(playerName));
			return;
		}

		sendInfo(admin,
				"\n[关于 " + target.getName() + " 的信息]\n-基本信息: lv" + target.getLevel() + "(" + target.getCommonData().getExpShown() + " 经验值), "
					+ target.getRace() + ", " + target.getPlayerClass() + "\n-IP: " + target.getClientConnection().getIP() + "\n" + "-账号名称: "
					+ target.getClientConnection().getAccount().getName() + "\n");

		if (params.length < 2)
			return;

		if (params[1].equals("item")) {
			StringBuilder strbld = new StringBuilder("-背包中的物品:");
			appendItems(strbld, target.getInventory().getItemsWithKinah());
			strbld.append("-已装备的物品:");
			appendItems(strbld, target.getEquipment().getEquippedItems());
			strbld.append("-仓库中的物品:");
			appendItems(strbld, target.getWarehouse().getItemsWithKinah());
			sendInfo(admin, strbld.toString());
		} else if (params[1].equals("group")) {
			final StringBuilder strbld = new StringBuilder("-队伍信息:\n\t队长: ");

			PlayerGroup group = target.getPlayerGroup();
			if (group == null)
				sendInfo(admin, "-队伍信息: 没有队伍");
			else {
				strbld.append(group.getLeader().getName() + "\n\t成员:\n");
				group.forEach(player -> strbld.append("\t\t" + player.getName() + "\n"));
				sendInfo(admin, strbld.toString());
			}
		} else if (params[1].equals("skills")) {
			StringBuilder strbld = new StringBuilder("-技能列表:\n");
			for (PlayerSkillEntry skill : target.getSkillList().getAllSkills())
				strbld.append("\tlevel " + skill.getSkillLevel() + " of " + DataManager.SKILL_DATA.getSkillTemplate(skill.getSkillId()).getName() + "\n");
			sendInfo(admin, strbld.toString());
		} else if (params[1].equals("loc")) {
			String chatLink = ChatUtil.position(target.getName(), target.getPosition());
			sendInfo(admin, "- " + chatLink + "的位置:\n\t" + target.getPosition().toCoordString());
		} else if (params[1].equals("legion")) {
			Legion legion = target.getLegion();
			if (legion == null)
				sendInfo(admin, "-军团信息: 没有加入军团");
			else {
				StringBuilder strbld = new StringBuilder();
				List<LegionMemberEx> legionmemblist = LegionService.getInstance().loadLegionMemberExList(legion, null);
				Iterator<LegionMemberEx> it = legionmemblist.iterator();
				strbld.append("-军团信息:\n\t名称: " + legion.getName() + ", 等级: " + legion.getLegionLevel() + "\n\t成员(在线):\n");
				while (it.hasNext()) {
					LegionMemberEx act = it.next();
					strbld.append("\t\t" + act.getName() + "(" + (act.isOnline() ? "在线" : "离线") + ")" + act.getRank().toString() + "\n");
				}
				sendInfo(admin, strbld.toString());
			}
		} else if (params[1].equals("ap")) {
			sendInfo(admin, target.getName() + "的深渊点数信息");
			sendInfo(admin, "总深渊点数 = " + target.getAbyssRank().getAp());
			sendInfo(admin, "总击杀数 = " + target.getAbyssRank().getAllKill());
			sendInfo(admin, "今日击杀数 = " + target.getAbyssRank().getDailyKill());
			sendInfo(admin, "今日深渊点数 = " + target.getAbyssRank().getDailyAP());
		} else if (params[1].equals("chars")) {
			sendInfo(admin, target.getName() + "的其他角色 (" + target.getClientConnection().getAccount().size() + ") :");
			target.getClientConnection().getAccount().forEach(d -> sendInfo(admin, d.getPlayerCommonData().getName()));
		} else if (params[1].equals("knownlist")) {
			sendInfo(admin, target.getName() + "的已知列表");
			target.getKnownList().forEachObject(obj -> sendInfo(admin, obj.getName() + " objectId:" + obj.getObjectId()));
		} else if (params[1].equals("visuallist")) {
			sendInfo(admin, target.getName() + "的可见列表");
			target.getKnownList().forEachVisibleObject(obj -> sendInfo(admin, obj.getName() + " objectId:" + obj.getObjectId()));
		} else {
			sendInfo(admin);
		}
	}

	private void appendItems(StringBuilder strbld, List<Item> items) {
		if (items.isEmpty())
			strbld.append("\nnone");
		else
			items.forEach(item -> strbld.append("\n\t" + item.getItemCount() + "x " + ChatUtil.item(item.getItemId())));
	}
}