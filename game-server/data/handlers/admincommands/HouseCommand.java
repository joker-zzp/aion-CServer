package admincommands;

import java.awt.Color;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.aionemu.gameserver.model.animations.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.services.teleport.TeleportService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * @author Rolandas
 */
public class HouseCommand extends AdminCommand {

	public HouseCommand() {
		super("house", "房屋传送和所有权管理。");

		// @formatter:off
		setSyntaxInfo(
			"list - 显示所有有房屋的地图。",
			"list <map> - 显示指定地图的所有房屋地址。",
			"tp <address> - 传送到指定地址的房屋。",
			"own <address> - 将指定房屋的所有权授予你的目标。",
			"revoke <address> - 撤销指定房屋的所有权。",
			"reloadscripts <address> - 重新加载指定房屋的所有脚本。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			sendInfo(admin);
			return;
		}

		House house = null;
		if (params.length >= 2) {
			int address = NumberUtils.toInt(params[1]);
			house = HousingService.getInstance().getHouseByAddress(address);
		}
		if (house == null && !"list".equalsIgnoreCase(params[0])) {
			sendInfo(admin, "无效的地址。");
			return;
		}
		if ("list".equalsIgnoreCase(params[0])) {
			if (params.length == 1)
				listMapsWithHouses(admin);
			else
				listHouses(admin, WorldMapType.of(params[1]));
		} else if ("own".equalsIgnoreCase(params[0])) {
			acquireHouse(admin, house);
		} else if ("revoke".equalsIgnoreCase(params[0])) {
			revokeOwnership(admin, house);
		} else if ("tp".equalsIgnoreCase(params[0])) {
			TeleportService.teleportTo(admin, house.getPosition().getWorldMapInstance(), house.getX(), house.getY(), house.getZ(),
				house.getTeleportHeading(), TeleportAnimation.NONE);
		} else if ("reloadscripts".equalsIgnoreCase(params[0])) {
			reloadPlayerScripts(admin, house);
		} else {
			sendInfo(admin);
		}
	}

	private void listMapsWithHouses(Player admin) {
		String maps = HousingService.getInstance().getCustomHouses().stream().map(house -> WorldMapType.getWorld(house.getAddress().getMapId()))
			.distinct().sorted().map(worldMapType -> ChatUtil.color(WordUtils.capitalizeFully(String.valueOf(worldMapType)), Color.WHITE))
			.collect(Collectors.joining("\n\t"));
		sendInfo(admin, "有房屋的地图:\n\t" + maps + "\n输入 " + ChatUtil.color(getAliasWithPrefix() + " list mapname", Color.WHITE)
			+ " 以显示该地图的所有房屋。");
	}

	private void listHouses(Player admin, WorldMapType worldMapType) {
		if (worldMapType == null) {
			sendInfo(admin, "无效的地图名称。");
			return;
		}
		Map<HouseType, List<House>> housesByType = getHousesByType(worldMapType.getId());
		if (housesByType.isEmpty()) {
			sendInfo(admin, WordUtils.capitalizeFully(worldMapType.toString()) + " 中没有房屋");
			return;
		}
		sendInfo(admin, WordUtils.capitalizeFully(worldMapType.toString()) + " 的房屋:");
		housesByType.forEach((houseType, houses) -> sendInfo(admin, WordUtils.capitalizeFully(houseType.toString()) + ":\n\t" + formatAddresses(houses)));
	}

	private String formatAddresses(List<House> houses) {
		boolean dash = false;
		int lastAddress = houses.get(0).getAddress().getId();
		String addresses = ChatUtil.color(lastAddress + "", Color.WHITE);
		for (int i = 1; i < houses.size(); i++) {
			House house = houses.get(i);
			int currentAddress = house.getAddress().getId();
			if (lastAddress + 1 != currentAddress || i + 1 == houses.size() || currentAddress + 1 != houses.get(i + 1).getAddress().getId()) {
				if (!addresses.isEmpty() && !dash)
					addresses += ", ";
				addresses += ChatUtil.color(currentAddress + "", Color.WHITE);
				dash = false;
			} else if (!dash) {
				addresses += "-";
				dash = true;
			}
			lastAddress = house.getAddress().getId();
		}
		return addresses;
	}

	private Map<HouseType, List<House>> getHousesByType(int mapId) {
		Comparator<House> comparator = Comparator.comparing(house -> house.getHouseType().getId());
		comparator = comparator.reversed().thenComparing(house -> house.getAddress().getId());
		return HousingService.getInstance().getCustomHouses().stream().filter(house -> house.getAddress().getMapId() == mapId).sorted(comparator)
			.collect(Collectors.groupingBy(House::getHouseType, LinkedHashMap::new, Collectors.toList()));
	}

	private void acquireHouse(Player admin, House house) {
		VisibleObject creature = admin.getTarget();
		if (!(creature instanceof Player target)) {
			PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
			return;
		}

		if (house.getOwnerId() == target.getObjectId()) {
			sendInfo(admin, target.getName() + " 已经拥有该房屋。");
			return;
		}
		if (target.getHouses().size() >= 2) {
			sendInfo(admin, target.getName() + " 必须先出售他当前处于宽限期的旧房屋！");
			return;
		}
		House studio = HousingService.getInstance().getPlayerStudio(target.getObjectId());
		if (studio != null)
			HousingService.getInstance().changeOwner(studio, 0);
		HousingService.getInstance().changeOwner(house, target.getObjectId());
		sendInfo(admin, "房屋 " + house.getName() + " 现在由 " + target.getName() + " 拥有");
	}

	private void revokeOwnership(Player admin, House house) {
		int ownerId = house.getOwnerId();
		if (ownerId == 0) {
			sendInfo(admin, "房屋没有所有者。");
			return;
		}
		HousingService.getInstance().changeOwner(house, 0);
		sendInfo(admin, "房屋 " + house.getAddress().getId() + " 的所有权已从 " + PlayerService.getPlayerName(ownerId) + " 处撤销");
	}

	private void reloadPlayerScripts(Player admin, House house) {
		Npc butler = house.getButler();
		if (butler == null) {
			sendInfo(admin, "地址为 " + house.getAddress().getId() + " 的房屋未找到管家");
			return;
		}
		house.reloadPlayerScripts();
		butler.getKnownList().forEachPlayer(house::sendScripts);
		sendInfo(admin, "脚本重载成功");
	}

}