package admincommands;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.model.templates.worldraid.WorldRaidLocation;
import com.aionemu.gameserver.services.WorldRaidService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 世界raid控制命令类 - 允许管理员启动/停止贝鲁特入侵事件
 * @author Whoop, Sykra
 */
public class WorldRaid extends AdminCommand {

	public WorldRaid() {
		super("worldraid", "启动/停止贝鲁特入侵事件");

		// @formatter:off
		setSyntaxInfo(
				"<help> - 显示世界raid命令的帮助信息。",
				"<list> - 显示所有可用的世界raid位置",
				"<active> - 显示所有活动中的世界raid位置",
				"<start> <location_id> - 在指定位置启动世界raid",
				"<stop> <location_id> - 在指定位置停止世界raid"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {
		if (!EventsConfig.ENABLE_WORLDRAID) {
			sendInfo(player, "世界raid当前已禁用。");
			return;
		}
		if (params.length < 1) {
			sendInfo(player);
			return;
		}

		if ("help".equalsIgnoreCase(params[0])) {
			sendInfo(player);
			return;
		} else if ("list".equalsIgnoreCase(params[0])) {
			sendInfo(player, createLocationList(DataManager.WORLD_RAID_DATA.getLocations().values(), "世界raid位置列表:"));
		} else if ("active".equalsIgnoreCase(params[0])) {
			sendInfo(player, createLocationList(WorldRaidService.getInstance().getActiveWorldRaidLocations(), "当前活动的世界raid:"));
		} else {
			if (params.length < 2 || !NumberUtils.isNumber(params[1])) {
				sendInfo(player);
				return;
			}

			int locationId = NumberUtils.toInt(params[1]);
			if (!WorldRaidService.getInstance().isValidWorldRaidLocation(locationId)) {
				sendInfo(player, "无效的世界raid位置: " + locationId);
				return;
			}

			if ("start".equalsIgnoreCase(params[0])) {
				if (WorldRaidService.getInstance().isWorldRaidInProgress(locationId)) {
					sendInfo(player, "位置 " + locationId + " 的世界raid已经在进行中");
					return;
				}
				sendInfo(player, "正在位置 " + locationId + " 启动世界raid");
				WorldRaidService.getInstance().startRaid(locationId, false);
			} else if ("stop".equalsIgnoreCase(params[0])) {
				if (!WorldRaidService.getInstance().isWorldRaidInProgress(locationId)) {
					sendInfo(player, "位置 " + locationId + " 的世界raid尚未启动。");
					return;
				}
				sendInfo(player, "已停止位置 " + locationId + " 的世界raid");
				WorldRaidService.getInstance().stopRaid(locationId);
			} else {
				sendInfo(player);
			}
		}
	}

	private String createLocationList(final Collection<WorldRaidLocation> locations, final String header) {
		final StringBuilder sb = new StringBuilder();
		if (header != null && !header.isEmpty())
			sb.append(header);
		if (locations == null || locations.isEmpty()) {
			sb.append("\n\t没有可用的位置！");
			return sb.toString();
		}

		Map<String, List<WorldRaidLocation>> locationsByMapId = locations.stream().collect(Collectors.groupingBy(worldRaidLocation -> {
			WorldMapTemplate mapTemplate = DataManager.WORLD_MAPS_DATA.getTemplate(worldRaidLocation.getMapId());
			if (mapTemplate == null || mapTemplate.getName().isEmpty())
				return String.valueOf(worldRaidLocation.getMapId());
			return mapTemplate.getName();
		}, Collectors.toList()));

		locationsByMapId.keySet().stream().sorted().forEach(mapName -> {
			List<WorldRaidLocation> locationsForMap = locationsByMapId.get(mapName);
			if (locationsForMap == null)
				return;
			sb.append("\n\t").append(ChatUtil.color(mapName, Color.WHITE)).append(" - ");
			sb.append(locationsForMap.stream().map(this::createPositionString).collect(Collectors.joining(", ")));
		});
		return sb.toString();
	}

	private String createPositionString(final WorldRaidLocation location) {
		return ChatUtil.position(String.valueOf(location.getLocationId()), location.getMapId(), location.getX(), location.getY(), location.getZ());
	}

}