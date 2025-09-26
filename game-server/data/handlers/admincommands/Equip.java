package admincommands;

import java.lang.reflect.Field;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.stats.listeners.ItemEquipmentListener;
import com.aionemu.gameserver.model.templates.item.GodstoneInfo;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.item.ItemType;
import com.aionemu.gameserver.model.templates.item.enums.ItemGroup;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemSocketService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Tago, Wakizashi
 */
public class Equip extends AdminCommand {

	public Equip() {
		super("equip");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length != 0) {
			int i = 0;
			if ("help".startsWith(params[i])) {
				if (params[i + 1] == null)
					showHelp(admin);
				else if ("socket".startsWith(params[i + 1]))
					showHelpSocket(admin);
				else if ("enchant".startsWith(params[i + 1]))
					showHelpEnchant(admin);
				else if ("godstone".startsWith(params[i + 1]))
					showHelpGodstone(admin);
				return;
			}
			Player player = null;
			player = World.getInstance().getPlayer(Util.convertName(params[i]));
			if (player == null) {
				VisibleObject target = admin.getTarget();
				if (target instanceof Player)
					player = (Player) target;
				else
					player = admin;
			} else
				i++;
			if ("socket".startsWith(params[i])) {
				int manastone = 167000551;
				int quant = 0;
				try {
					manastone = params[i + 1] == null ? manastone : Integer.parseInt(params[i + 1]);
					quant = params[i + 2] == null ? quant : Integer.parseInt(params[i + 2]);
				} catch (Exception ex2) {
					showHelpSocket(admin);
					return;
				}
				socket(admin, player, manastone, quant);
				return;
			}
			if ("enchant".startsWith(params[i])) {
				int enchant = 0;
				try {
					enchant = params[i + 1] == null ? enchant : Integer.parseInt(params[i + 1]);
				} catch (Exception ex) {
					showHelpEnchant(admin);
					return;
				}
				enchant(admin, player, enchant);
				return;
			}
			if ("tamper".startsWith(params[i])) {
				int tampering = 0;
				try {
					tampering = params[i + 1] == null ? tampering : Integer.parseInt(params[i + 1]);
				} catch (Exception ex) {
					showHelpEnchant(admin);
					return;
				}
				tamper(admin, player, tampering);
				return;
			}
			if ("godstone".startsWith(params[i])) {
				int godstone = 100;
				try {
					godstone = params[i + 1] == null ? godstone : Integer.parseInt(params[i + 1]);
				} catch (Exception ex) {
					showHelpGodstone(admin);
					return;
				}
				godstone(admin, player, godstone);
				return;
			}
		}
		showHelp(admin);
	}

	private void socket(Player admin, Player player, int manastone, int quant) {
		if (manastone != 0 && (manastone < 167000000 || manastone > 168000000)) {
			sendInfo(admin, "您应该提供魔石的物品ID, 或使用0移除所有魔石.");
			return;
		}
		for (Item targetItem : player.getEquipment().getEquippedItemsWithoutStigma()) {
			if (isUpgradeble(targetItem)) {
				if (manastone == 0) {
					ItemEquipmentListener.removeStoneStats(targetItem.getItemStones(), player.getGameStats());
					ItemSocketService.removeAllManastone(player, targetItem);
				} else {
					int counter = quant <= 0 ? getMaxSlots(targetItem) : quant;
					while (targetItem.getItemStones().size() < getMaxSlots(targetItem) && counter >= 0) {
						ManaStone manaStone = ItemSocketService.addManaStone(targetItem, manastone, false);
						ItemEquipmentListener.addStoneStats(targetItem, manaStone, player.getGameStats());
						counter--;

					}
				}
				PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
				ItemPacketService.updateItemAfterInfoChange(player, targetItem);
				targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
			}

		}
		if (manastone == 0) {
			if (player == admin)
				sendInfo(player, "已移除所有已装备物品上的魔石");
			else {
				sendInfo(admin, "已移除玩家" + player.getName() + "所有已装备物品上的魔石");
				sendInfo(player, "管理员" + admin.getName() + "移除了您所有已装备物品上的魔石");
			}
		} else {
			if (player == admin)
				sendInfo(player, quant + "个[物品: " + manastone + "]已添加到所有已装备物品的空闲槽位中");
			else {
				sendInfo(admin, quant + "个[物品: " + manastone + "]已添加到玩家" + player.getName() + "所有已装备物品的空闲槽位中");
				sendInfo(player, "管理员" + admin.getName() + "添加了" + quant + "个[物品: " + manastone + "]到您所有已装备物品的空闲槽位中");
			}
		}
	}

	private void godstone(Player admin, Player player, int godstone) {
		Item targetItem = player.getEquipment().getMainHandWeapon();
		if (godstone > 100000000) {
			ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(godstone);
			GodstoneInfo godstoneInfo = itemTemplate.getGodstoneInfo();
			if (godstoneInfo == null) {
				sendInfo(admin, "错误的神石物品ID");
				return;
			}
			targetItem.addGodStone(godstone);
			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
			ItemPacketService.updateItemAfterInfoChange(player, targetItem);
			targetItem.setPersistentState(PersistentState.UPDATE_REQUIRED);
			if (player == admin)
				sendInfo(player, "[物品: " + godstone + "]已镶嵌到您装备的主手武器[物品: " + targetItem.getItemId() + "]中");
			else {
				sendInfo(admin, "[物品: " + godstone + "]已镶嵌到玩家" + player.getName() + "装备的主手武器[物品: " + targetItem.getItemId() + "]中");
				sendInfo(player, "管理员" + admin.getName() + "将[物品: " + godstone + "]镶嵌到您装备的主手武器[物品: " + targetItem.getItemId() + "]中");
			}
		} else if (targetItem.getGodStone() != null) {
			try {
				if (godstone <= 100)
					godstone *= 10;
				if (godstone > 1000)
					godstone = 1000;
				Class<?> gs = targetItem.getGodStone().getClass();
				Field probability = gs.getDeclaredField("probability");
				Field probabilityLeft = gs.getDeclaredField("probability");
				probability.setAccessible(true);
				probabilityLeft.setAccessible(true);
				probability.setInt(targetItem.getGodStone(), godstone);
				probabilityLeft.setInt(targetItem.getGodStone(), godstone);
			} catch (Exception ex2) {
				sendInfo(admin, "发生错误。");
				return;
			}
			if (player.equals(admin))
				sendInfo(player, "您主手武器上的神石现在将有约" + (godstone / 10) + "%的几率激活.");
			else {
				sendInfo(admin, "玩家" + player.getName() + "主手武器上的神石现在将有约" + godstone + "%的几率激活.");
				sendInfo(player, "管理员" + admin.getName() + "祝福了您主手武器上的神石，现在将有约" + godstone + "%的几率激活.");
			}
		}
	}

	private void enchant(Player admin, Player player, int enchant) {
		for (Item targetItem : player.getEquipment().getEquippedItemsWithoutStigma()) {
			if (isUpgradeble(targetItem)) {
				if (targetItem.getEnchantLevel() == enchant)
					continue;
				if (enchant > 255)
					enchant = 255;
				if (enchant < 0)
					enchant = 0;

				targetItem.setEnchantLevel(enchant);
				if (targetItem.isEquipped()) {
					player.getGameStats().updateStatsVisually();
				}
				ItemPacketService.updateItemAfterInfoChange(player, targetItem, ItemUpdateType.STATS_CHANGE);
			}
		}
		if (player == admin)
			sendInfo(player, "所有已装备物品已强化至" + enchant + "级");
		else {
			sendInfo(admin, "玩家" + player.getName() + "的所有已装备物品已强化至" + enchant + "级");
			sendInfo(player, "管理员" + admin.getName() + "将您所有已装备物品强化至" + enchant + "级");
		}

	}

	private void tamper(Player admin, Player player, int tampering) {
		for (Item targetItem : player.getEquipment().getEquippedItemsWithoutStigma()) {
			if (isTampering(targetItem)) {
				if (targetItem.getTempering() == tampering)
					continue;
				if (tampering > 255)
					tampering = 255;
				if (tampering < 0)
					tampering = 0;

				targetItem.setTempering(tampering);
				if (targetItem.isEquipped()) {
					player.getGameStats().updateStatsVisually();
				}
				ItemPacketService.updateItemAfterInfoChange(player, targetItem, ItemUpdateType.STATS_CHANGE);
			}
		}
		if (player == admin)
			sendInfo(player, "所有已装备物品已增幅至" + tampering + "级");
		else {
			sendInfo(admin, "玩家" + player.getName() + "的所有已装备物品已增幅至" + tampering + "级");
			sendInfo(player, "管理员" + admin.getName() + "将您所有已装备物品增幅至" + tampering + "级");
		}

	}

	/**
	 * 检查物品是否可强化和/或可镶嵌
	 */
	public static boolean isUpgradeble(Item item) {
		if (item.getItemTemplate().isNoEnchant())
			return false;
		if (item.getItemTemplate().isWeapon())
			return true;
		if (item.getItemTemplate().isArmor()) {
			long at = item.getItemTemplate().getItemSlot();
			if (at == 1 || /* 主手 */
				at == 2 || /* 副手 */
				at == 8 || /* 上衣 */
				at == 16 || /* 手套 */
				at == 32 || /* 靴子 */
				at == 2048 || /* 护肩 */
				at == 4096 || /* 裤子 */
				at == 131072 || /* 主副手 */
				at == 262144) /* 副副手 */
				return true;
		}
		return false;
	}

	public static boolean isTampering(Item item) {
		if (item.getItemTemplate().getItemGroup() == ItemGroup.EARRING)
			return true;
		if (item.getItemTemplate().getItemGroup() == ItemGroup.RING)
			return true;
		if (item.getItemTemplate().getItemGroup() == ItemGroup.NECKLACE)
			return true;
		if (item.getItemTemplate().getItemGroup() == ItemGroup.BELT)
			return true;
		if (item.getItemTemplate().getItemGroup() == ItemGroup.HEAD)
			return true;
		if (item.getItemTemplate().getItemGroup() == ItemGroup.PLUME)
			return true;
		return false;
	}

	/**
	 * 返回可镶嵌的最大魔石数量
	 */
	public static int getMaxSlots(Item item) {
		int slots = 0;
		switch (item.getItemTemplate().getItemQuality()) {
		case COMMON:
		case JUNK:
			slots = 1;
			break;
		case RARE:
			slots = 2;
			break;
		case LEGEND:
			slots = 3;
			break;
		case UNIQUE:
			slots = 4;
			break;
		case EPIC:
			slots = 5;
			break;
		case MYTHIC:
			slots = 5;
			break;
		default:
			slots = 0;
			break;
		}
		if (item.getItemTemplate().getItemType() == ItemType.DRACONIC)
			slots += 1;
		if (item.getItemTemplate().getItemType() == ItemType.ABYSS)
			slots += 2;
		return slots;

	}

	private void showHelp(Player admin) {
        sendInfo(admin,
            "[帮助: 装备命令]\n" + " 使用//equip help <socket|enchant|godstone>获取命令的更多详细信息.\n" + " 注意: 此命令使用智能匹配. 您可以缩写大多数命令.\n" + " 例如: (//equip so 167000551 5) 将匹配到 (//equip socket 167000551 5)");
    }

	private void showHelpEnchant(Player admin) {
        sendInfo(admin,
            "语法: //equip [玩家名称] enchant [强化等级 = 0]\n" + " 此命令将所有已装备物品强化至指定等级(最高255级).\n" + " 注意: 您可以省略[]中的参数，尤其是玩家名称.\n" + " 目标: 指定的玩家，然后是目标玩家，最后是自己.\n" + " 默认值: 强化等级为0.");
    }

	private void showHelpSocket(Player admin) {
        sendInfo(admin, "语法: //equip [玩家名称] socket [魔石ID = 167000551] [数量 = 0]\n" + " 此命令将使用给定的魔石ID镶嵌所有已装备物品的空闲槽位.\n" + " 使用魔石ID = 0移除所有魔石. 数量 = 0表示填充所有空闲槽位.\n" + " 注意: 您可以省略[]中的参数，尤其是玩家名称.\n" + " 目标: 指定的玩家，然后是目标玩家，最后是自己.\n" + " 默认值: 魔石ID为167000551，数量为0表示填充所有槽位.");
    }

	private void showHelpGodstone(Player admin) {
        sendInfo(admin,
            "语法: //equip [玩家名称] godstone [几率 = 100|神石ID]\n" + " 此命令将神石激活几率更改为给定数值(0-100).\n" + " 提供神石物品ID将其镶嵌到您的主手武器上.\n" + " 注意: 您可以省略[]中的参数，尤其是玩家名称.\n" + " 目标: 指定的玩家，然后是目标玩家，最后是自己.\n" + " 默认值: 几率为100，这是默认操作.");
    }

	@Override
	public void info(Player player, String message) {
		showHelp(player);
	}
}