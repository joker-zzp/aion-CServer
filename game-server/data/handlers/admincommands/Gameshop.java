package admincommands;

import com.aionemu.gameserver.dao.InGameShopDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TOLL_INFO;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.network.loginserver.serverpackets.SM_ACCOUNT_TOLL_INFO;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;

/**
 * @author xTz
 */
public class Gameshop extends AdminCommand {

	public Gameshop() {
		super("gameshop", "管理游戏商城相关功能.");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			info(admin, null);
			return;
		}
		int itemId = 0;
		int list;
		byte category, subCategory, itemType, gift;
		long count, price, toll;
		Player player;
		String titleDescription;
		if ("delete".startsWith(params[0])) {
			try {
				itemId = Integer.parseInt(params[1]);
				category = Byte.parseByte(params[2]);
				subCategory = Byte.parseByte(params[3]);
				list = Integer.parseInt(params[4]);
			} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "<itemId, category, subCategory, list> 值必须是 int, byte, byte, int 类型.");
				return;
			}
			InGameShopDAO.deleteIngameShopItem(itemId, category, subCategory, list - 1);
			PacketSendUtility.sendMessage(admin, "已移除物品 [item:" + itemId + "]");
		} else if ("add".startsWith(params[0])) {
			try {
				itemId = Integer.parseInt(params[1]);
				count = Long.parseLong(params[2]);
				price = Long.parseLong(params[3]);
				category = Byte.parseByte(params[4]);
				subCategory = Byte.parseByte(params[5]);
				itemType = Byte.parseByte(params[6]);
				gift = Byte.parseByte(params[7]);
				list = Integer.parseInt(params[8]);
				titleDescription = Util.convertName(params[9]);
			} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, 
					"<itemId, count, price, category, subCategory, itemType, gift, list, description> 值必须是 int, long, long, byte, byte, byte, byte, int, string, Object... 类型.");
				return;
			}

			ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);
			if (itemTemplate == null) {
				PacketSendUtility.sendMessage(admin, "物品ID不正确: " + itemId);
				return;
			}

			String description = "";

			for (int i = 10; i < params.length; i++) {
				description += Util.convertName(params[i]) + " ";
			}
			description = description.trim();

			if (list < 1) {
				PacketSendUtility.sendMessage(admin, "<list> : 最小值为1.");
				return;
			}
			if (gift < 0 || gift > 1) {
				PacketSendUtility.sendMessage(admin, "<gift> : 最小值为0, 最大值为1.");
				return;
			}
			if (itemType < 0 || itemType > 2) {
				PacketSendUtility.sendMessage(admin, "<itemType> : 最小值为0, 最大值为2.");
				return;
			}
			if (subCategory < 3 || subCategory > 19) {
				PacketSendUtility.sendMessage(admin, "<category> : 最小值为3, 最大值为19.");
				return;
			}
			if (titleDescription.length() > 20) {
				PacketSendUtility.sendMessage(admin, "<title description> : 最大长度为20.");
				return;
			}
			if (titleDescription.equals("empty")) {
				titleDescription = "";
			}
			InGameShopDAO.saveIngameShopItem(IDFactory.getInstance().nextId(), itemId, count, price, category, subCategory,
				list - 1, 1, itemType, gift, titleDescription, description);
			PacketSendUtility.sendMessage(admin, "已添加物品 [item:" + itemId + "]");
		} else if ("deleteranking".startsWith(params[0])) {
			try {
				itemId = Integer.parseInt(params[1]);
			} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "<itemId> 值必须是整数。");
			}
			InGameShopDAO.deleteIngameShopItem(itemId, (byte) -1, (byte) -1, -1);
			PacketSendUtility.sendMessage(admin, "已从排行榜销售中移除物品 [item:" + itemId + "]");
		} else if ("addranking".startsWith(params[0])) {
			try {
				itemId = Integer.parseInt(params[1]);
				count = Long.parseLong(params[2]);
				price = Long.parseLong(params[3]);
				itemType = Byte.parseByte(params[4]);
				gift = Byte.parseByte(params[5]);
				titleDescription = Util.convertName(params[6]);
			} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, 
					"<itemId, count, price, itemType, gift, description> 值必须是 int, long, long, byte, byte, string, Object... 类型.");
				return;
			}
			String description = "";
			for (int i = 7; i < params.length; i++) {
				description += Util.convertName(params[i]) + " ";
			}
			description = description.trim();

			ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(itemId);

			if (itemTemplate == null) {
				PacketSendUtility.sendMessage(admin, "Item id is incorrect: " + itemId);
				return;
			}
			if (titleDescription.equals("empty")) {
				titleDescription = "";
			}
			InGameShopDAO.saveIngameShopItem(IDFactory.getInstance().nextId(), itemId, count, price, (byte) -1, (byte) -1, -1, 0,
				itemType, gift, titleDescription, description);
			PacketSendUtility.sendMessage(admin, "You remove from Ranking Sales [item:" + itemId + "]");
		} else if ("settoll".startsWith(params[0])) {
			if (params.length == 3) {
				try {
					toll = Integer.parseInt(params[2]);
				} catch (NumberFormatException e) {
					PacketSendUtility.sendMessage(admin, "<toll> value must be an integer.");
					return;
				}

				String name = Util.convertName(params[1]);

				player = World.getInstance().getPlayer(name);
				if (player == null) {
					PacketSendUtility.sendMessage(admin, "指定的玩家不在线.");
					return;
				}

				if (LoginServer.getInstance().sendPacket(new SM_ACCOUNT_TOLL_INFO(toll, player.getAccount().getId()))) {
					player.getClientConnection().getAccount().setToll(toll);
					PacketSendUtility.sendPacket(player, new SM_TOLL_INFO(toll));
					PacketSendUtility.sendMessage(admin, "已将点券设置为 " + toll + ".");
				} else
					PacketSendUtility.sendMessage(admin, "登录服务器通信错误.");
			}
			if (params.length == 2) {
				try {
					toll = Integer.parseInt(params[1]);
				} catch (NumberFormatException e) {
					PacketSendUtility.sendMessage(admin, "<toll> value must be an integer.");
					return;
				}

				if (toll < 0) {
				PacketSendUtility.sendMessage(admin, "<toll> 必须大于0.");
				return;
			}

				VisibleObject target = admin.getTarget();
				if (!(target instanceof Player)) {
					PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
					return;
				}

				player = (Player) target;
				if (LoginServer.getInstance().sendPacket(new SM_ACCOUNT_TOLL_INFO(toll, player.getAccount().getId()))) {
					player.getClientConnection().getAccount().setToll(toll);
					PacketSendUtility.sendPacket(player, new SM_TOLL_INFO(toll));
					PacketSendUtility.sendMessage(admin, "Tolls setted to " + toll + ".");
				} else
					PacketSendUtility.sendMessage(admin, "ls communication error.");
			}
		} else if ("addtoll".startsWith(params[0])) {
			if (params.length == 3) {
				try {
					toll = Integer.parseInt(params[2]);
				} catch (NumberFormatException e) {
					PacketSendUtility.sendMessage(admin, "<toll> value must be an integer.");
					return;
				}

				if (toll < 0) {
					PacketSendUtility.sendMessage(admin, "<toll> must > 0.");
					return;
				}

				String name = Util.convertName(params[1]);

				player = World.getInstance().getPlayer(name);
				if (player == null) {
					PacketSendUtility.sendMessage(admin, "The specified player is not online.");
					return;
				}

				PacketSendUtility.sendMessage(admin, "已向玩家 " + name + " 添加 " + toll + " 点券");
				InGameShopEn.getInstance().addToll(player, toll);
			}
			if (params.length == 2) {
				try {
					toll = Integer.parseInt(params[1]);
				} catch (NumberFormatException e) {
					PacketSendUtility.sendMessage(admin, "<toll> value must be an integer.");
					return;
				}

				VisibleObject target = admin.getTarget();
				if (!(target instanceof Player)) {
					PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_INVALID_TARGET());
					return;
				}

				player = (Player) target;
				PacketSendUtility.sendMessage(admin, "已向玩家 " + player.getName() + " 添加 " + toll + " 点券");
				InGameShopEn.getInstance().addToll(player, toll);
			}
		} else {
			PacketSendUtility.sendMessage(admin, "只能使用 addtoll、settoll、deleteranking、addranking、delete 或 add 命令。");
		}
	}

	@Override
	public void info(Player player, String message) {
		PacketSendUtility.sendMessage(player,
			"未检测到参数，请使用以下命令：\n"
				+ "//gameshop add <物品ID> <数量> <价格> <分类> <子分类> <物品类型> <礼物> <列表> <标题描述|empty> <物品描述|null>\n"
				+ "//gameshop delete <物品ID> <分类> <子分类> <列表>\n"
				+ "//gameshop addranking <物品ID> <数量> <价格> <物品类型> <礼物> <标题描述|empty> <物品描述|null>\n"
				+ "//gameshop deleteranking <物品ID>\n" + "//gameshop settoll <目标|玩家名> <点券数>\n" + "//gameshop addtoll <目标|玩家名> <点券数>");
	}
}