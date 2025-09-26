package admincommands;

import java.util.List;
import java.util.stream.Collectors;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.walker.WalkerTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GATHERABLE_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * @author KID, Rolandas, Neon
 * 更新生成点数据命令
 */
public class SpawnUpdate extends AdminCommand {

	public SpawnUpdate() {
		super("spawnu", "更新生成点数据");

		setSyntaxInfo(
			"<x|y|z|h> [值] - 更新选定NPC/可采集物的X、Y、Z坐标或朝向（默认：使用您当前位置，可选：指定数值）",
			"<xyz|xyzh> - 将选定NPC/可采集物的位置或位置和朝向更新为您自己的位置",
			"<w> [walker_id] - 设置选定NPC的行走路径数据（默认：移除行走路径数据，可选：设置NPC的行走路径ID）",
			"help - 显示帮助信息");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}
		if (params[0].equalsIgnoreCase("help")) {
			sendInfo(admin);
			return;
		}

		VisibleObject target = admin.getTarget();
		if (target instanceof Npc && params[0].equalsIgnoreCase("w")) {
			updateWalker(admin, (Npc) target, params.length == 2 ? params[1].toUpperCase() : null);
			return;
		}

		if (!(target instanceof Npc) && !(target instanceof Gatherable)) {
			PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
			return;
		}

		Float x = null, y = null, z = null;
		Byte h = null;
		if (params[0].equalsIgnoreCase("xyz")) {
			x = admin.getX();
			y = admin.getY();
			z = admin.getZ();
		} else if (params[0].equalsIgnoreCase("xyzh")) {
			x = admin.getX();
			y = admin.getY();
			z = admin.getZ();
			h = admin.getHeading();
		} else if (params[0].equalsIgnoreCase("x")) {
			if (params.length == 1)
				x = admin.getX();
			else
				x = Float.parseFloat(params[1]);
		} else if (params[0].equalsIgnoreCase("y")) {
			if (params.length == 1)
				y = admin.getY();
			else
				y = Float.parseFloat(params[1]);
		} else if (params[0].equalsIgnoreCase("z")) {
			if (params.length == 1)
				z = admin.getZ();
			else
				z = Float.parseFloat(params[1]);
		} else if (params[0].equalsIgnoreCase("h")) {
			if (params.length == 1)
				h = admin.getHeading();
			else
				h = Byte.parseByte(params[1]);
		} else {
			sendInfo(admin);
			return;
		}

		WorldPosition tPos = target.getPosition();
		tPos.setXYZH(x, y, z, h);

		if (target instanceof Npc npc)
			PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(npc, admin));
		else
			PacketSendUtility.sendPacket(admin, new SM_GATHERABLE_INFO(target));
		sendInfo(admin,
			"已更新 " + target.getClass().getSimpleName() + " 的坐标为\nX:" + tPos.getX() + " Y:" + tPos.getY() + " Z:" + tPos.getZ() + " H:"+ tPos.getHeading() + "。");

		if (!DataManager.SPAWNS_DATA.saveSpawn(target, false))
			sendInfo(admin, "无法保存生成点。可能是特殊或临时生成点（攻城战、基地、入侵等），无法被修改。");
	}

	private void updateWalker(Player admin, Npc target, String walkerId) {
		SpawnTemplate spawn = target.getSpawn();
		String oldId = spawn.getWalkerId();
		if (oldId == null && walkerId == null) {
			sendInfo(admin, "NPC没有可以移除的行走路径ID。");
		} else {
			if (walkerId != null) {
				WalkerTemplate template = DataManager.WALKER_DATA.getWalkerTemplate(walkerId);
				if (template == null) {
					sendInfo(admin, "npc_walker.xml中不存在该模板。");
					return;
				}
				List<SpawnGroup> allSpawns = DataManager.SPAWNS_DATA.getSpawnsByWorldId(target.getWorldId());
				List<SpawnTemplate> allSpots = allSpawns.stream().flatMap(s -> s.getSpawnTemplates().stream()).collect(Collectors.toList());
				List<SpawnTemplate> sameIds = allSpots.stream().filter(s -> s.getWalkerId().equals(walkerId)).collect(Collectors.toList());
				if (sameIds.size() >= template.getPool()) {
						sendInfo(admin, "无法分配，行走路径池已达到上限。");
						return;
					}
			}
			spawn.setWalkerId(walkerId);
			PacketSendUtility.sendPacket(admin, new SM_DELETE(target));
			PacketSendUtility.sendPacket(admin, new SM_NPC_INFO(target, admin));
			if (walkerId == null)
					sendInfo(admin, "已移除NPC的行走路径ID " + oldId + "（NPC ID：" + target.getNpcId() + "）。");
				else
					sendInfo(admin, "已更新NPC的行走路径ID，从 " + oldId + " 更改为 " + walkerId + "。");
				if (!DataManager.SPAWNS_DATA.saveSpawn(target, false))
					sendInfo(admin, "无法保存生成点。");
		}
	}
}