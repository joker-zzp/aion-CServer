package admincommands;

import com.aionemu.gameserver.model.base.Base;
import com.aionemu.gameserver.model.base.BaseOccupier;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

public class BaseCommand extends AdminCommand {

	private static final String COMMAND_LIST = "list";
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";
	private static final String COMMAND_CAPTURE = "capture";
	private static final String COMMAND_ASSAULT = "assault";

	public BaseCommand() {
		super("base");

		// @formatter:off
		setSyntaxInfo(
				"<list> - 列出所有可用的基地位置及其占有者",
				"<capture> [id] [occupier] - 使用指定的新占有者占领指定基地.",
				"<assault> [id] - 为指定基地生成攻击者NPC(如果有)."
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			sendInfo(player, "参数不足。");
			return;
		}

		switch (params[0].toLowerCase()) {
			case COMMAND_LIST -> showBaseLocationList(player, params);
			case COMMAND_START -> startBase(player, params);
			case COMMAND_STOP -> stopBase(player, params);
			case COMMAND_CAPTURE -> captureBase(player, params);
			case COMMAND_ASSAULT -> assaultBase(player, params);
		}
	}

	protected void showBaseLocationList(Player player, String[] params) {
		BaseService.getInstance().getBaseLocations().values()
				.forEach(loc -> PacketSendUtility.sendMessage(player, "基地: %d 属于 %s".formatted(loc.getId(), loc.getOccupier())));
	}

	private void startBase(Player player, String[] params) {
		int baseId = parseBaseId(player, params);
		if (baseId == 0)
			return;

		if (BaseService.getInstance().isActive(baseId)) {
			sendInfo(player, "无需操作，它已经处于活跃状态。 [id=%d]".formatted(baseId));
			return;
		}
		BaseService.getInstance().start(baseId);
	}

	private void stopBase(Player player, String[] params) {
		int baseId = parseBaseId(player, params);
		if (baseId == 0)
			return;

		if (!BaseService.getInstance().isActive(baseId)) {
			sendInfo(player, "无需操作，它未处于活跃状态。 [id=%d]".formatted(baseId));
			return;
		}
		BaseService.getInstance().stop(baseId);
	}

	protected void captureBase(Player player, String[] params) {
		int baseId = parseBaseId(player, params);
		if (baseId == 0)
			return;

		if (!BaseService.getInstance().isActive(baseId)) {
			sendInfo(player, "[id=%d] 只有在活跃状态下才能被占领".formatted(baseId));
			return;
		}

		BaseOccupier occupier = getOccupier(params[2].toUpperCase());
		if (occupier == null) {
			sendInfo(player, params[2] + " 不是有效的占有者");
			return;
		}

		BaseService.getInstance().capture(baseId, occupier);
	}

	protected void assaultBase(Player player, String[] params) {
		int baseId = parseBaseId(player, params);
		if (baseId == 0)
			return;

		if (!BaseService.getInstance().isActive(baseId)) {
			sendInfo(player, "[id=%d] 只有在活跃状态下才能被攻击".formatted(baseId));
			return;
		}

		BaseOccupier occupier = getOccupier(params[2].toUpperCase());
		if (occupier == null) {
			sendInfo(player, params[2] + " 不是有效的占有者");
			return;
		}

		// assault
		Base<?> base = BaseService.getInstance().getActiveBase(baseId);
		if (base != null) {
			if (base.isUnderAssault())
				PacketSendUtility.sendMessage(player, "攻击已经开始！");
			else
				base.spawnBySpawnHandler(SpawnHandlerType.ATTACKER, occupier);
		}
	}

	private int parseBaseId(Player admin, String[] params) {
		if (params.length < 2) {
			sendInfo(admin, "参数不足");
			return 0;
		}

		int baseId;
		try {
			baseId = Integer.parseInt(params[1]);
		} catch (NumberFormatException e) {
			sendInfo(admin, "这个baseId不是一个数字。");
			return 0;
		}

		if (!BaseService.getInstance().getBaseLocations().containsKey(baseId)) {
			sendInfo(admin, "这个baseId不存在。");
			return 0;
		}

		return baseId;
	}

	private BaseOccupier getOccupier(String param) {
		try {
			return BaseOccupier.valueOf(param);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}