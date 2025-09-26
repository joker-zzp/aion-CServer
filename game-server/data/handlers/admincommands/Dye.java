package admincommands;

import java.awt.Color;
import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_PLAYER_APPEARANCE;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author loleron, Neon
 */
public class Dye extends AdminCommand {

	public Dye() {
		super("dye", "为玩家的可见装备染色。");

		setSyntaxInfo("<color> - 将选中的玩家染成指定颜色(可以是染色剂物品链接/ID、颜色名称或颜色十六进制代码)."
				+ "0 移除所有染色.");
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			sendInfo(player);
			return;
		}

		Player target = player.getTarget() instanceof Player p ? p : player;
		Integer itemColor = null; // null = 默认物品颜色
		String colorText = "default";
		String colorParam = params[0];

		if (!"0".equalsIgnoreCase(colorParam)) {
			// 尝试获取染色物品的 itemId
			itemColor = ChatUtil.getItemId(colorParam);
			ItemTemplate dyeItemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemColor);

			if (itemColor != 0 && dyeItemTemplate != null && dyeItemTemplate.getActions() != null && dyeItemTemplate.getActions().getDyeAction() != null) {
				itemColor = dyeItemTemplate.getActions().getDyeAction().getColor();
				colorText = ChatUtil.item(dyeItemTemplate.getTemplateId());
			} else {
				try {
					try {
						// 尝试通过名称获取颜色
						itemColor = ((Color) Class.forName("java.awt.Color").getField(colorParam.toUpperCase()).get(null)).getRGB();
					} catch (Exception e) {
						// 尝试通过十六进制代码获取颜色
						if (colorParam.length() <= 8) {
							if (colorParam.startsWith("#"))
								colorParam = colorParam.substring(1);
							else if (colorParam.startsWith("0x") || colorParam.startsWith("0X"))
								colorParam = colorParam.substring(2);
						}
						itemColor = Integer.valueOf(colorParam, 16);
					}
					colorText = ChatUtil.color("#" + String.format("%06X", itemColor & 0xFFFFFF), itemColor);
				} catch (NumberFormatException e) {
					sendInfo(player, "无效的颜色.");
					return;
				}
			}
		}

		List<Item> appearanceItems = target.getEquipment().getEquippedForAppearance();
		if (appearanceItems.isEmpty()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CHANGE_ITEM_SKIN_NO_TARGET_ITEM());
			return;
		}
		if (appearanceItems.stream().noneMatch(item -> item.getItemTemplate().isItemDyePermitted())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_COLOR_CHANGE_ERROR_CANNOTDYE(appearanceItems.get(0).getL10n()));
			return;
		}
		for (Item item : appearanceItems) {
			if (item.getItemSkinTemplate().isItemDyePermitted())
				item.setItemColor(itemColor);
			ItemPacketService.updateItemAfterInfoChange(target, item);
		}
		PacketSendUtility.broadcastPacket(target, new SM_UPDATE_PLAYER_APPEARANCE(target.getObjectId(), appearanceItems), true);
		target.getEquipment().setPersistentState(PersistentState.UPDATE_REQUIRED);

		if (itemColor == null)
			sendInfo(player, "已移除 " + target.getName() + " 的可见装备上的染色.");
		else
			sendInfo(player, "已为 " + target.getName() + " 染色(颜色：" + colorText + ").");

		if (!target.equals(player))
			sendInfo(target, player.getName() + " 将你可见装备的颜色更改为：" + colorText);
	}
}