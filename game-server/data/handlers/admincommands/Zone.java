package admincommands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 区域信息命令类 - 允许管理员查看目标所在区域的详细信息
 * @author ATracer
 */
public class Zone extends AdminCommand {

	public Zone() {
		super("zone");

		// @formatter:off
		setSyntaxInfo(
				"help - 显示此帮助信息",
				"[区域名称] - 显示目标当前所在区域的信息（默认：所有区域，可选：按指定区域名称过滤）",
				"<refresh> - 刷新你的区域信息"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length > 1) {
			sendInfo(admin);
			return;
		}
		if (params.length == 1 && "help".equalsIgnoreCase(params[0])) {
			sendInfo(admin);
			return;
		}
		if (params.length == 1 && "refresh".equalsIgnoreCase(params[0])) {
			admin.revalidateZones();
			sendInfo(admin, "区域信息已刷新");
			return;
		}
		Creature target = admin.getTarget() instanceof Creature creature ? creature : admin;
		String zoneNameParam = params.length == 0 ? null : params[0];
		List<ZoneInstance> zones = findZones(target, zoneNameParam);
		String zoneTypes = Arrays.stream(ZoneType.values()).filter(target::isInsideZoneType).map(ZoneType::name).collect(Collectors.joining(", "));
		if (!zoneTypes.isEmpty())
			sendInfo(admin, target.getName() + "的区域类型: " + zoneTypes);
		if (zones.isEmpty()) {
			sendInfo(admin, target.getName() + " 不在" + (zoneNameParam == null ? "任何区域" : zoneNameParam) + "中");
		} else {
			sendInfo(admin, target.getName() + "的" + (zones.size() == 1 ? "区域" : "区域列表") + ':');
			for (ZoneInstance zone : zones) {
				sendInfo(admin, zone.getAreaTemplate().getZoneName().name());
				sendInfo(admin, "飞行: " + zone.canFly() + "; 滑翔: " + zone.canGlide());
				sendInfo(admin, "骑乘: " + zone.canRide() + "; 飞行骑乘: " + zone.canFlyRide());
				sendInfo(admin, "奇斯克: " + zone.canPutKisk() + "; 召回: " + zone.canRecall());
				sendInfo(admin, "同种族决斗: " + zone.isSameRaceDuelsAllowed() + "; 其他种族决斗: " + zone.isOtherRaceDuelsAllowed());
				sendInfo(admin, "PvP: " + zone.isPvpAllowed());
				sendInfo(admin, "可返回战场: " + zone.canReturnToBattle());
			}
		}
	}

	private List<ZoneInstance> findZones(Creature creature, String zoneNameFilter) {
		List<ZoneInstance> zones = creature.findZones();
		if (zoneNameFilter != null) {
			ZoneName zoneName = ZoneName.get(zoneNameFilter);
			if (zoneName == ZoneName.NONE)
				throw new IllegalArgumentException("无效的区域名称");
			zones = zones.stream().filter(zone -> zone.getZoneTemplate().getName() == zoneName).toList();
		}
		return zones;
	}
}