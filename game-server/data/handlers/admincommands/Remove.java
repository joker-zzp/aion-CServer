package admincommands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Phantom, ATracer
 */
public class Remove extends AdminCommand {

	public Remove() {
		super("remove");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0 || "help".equals(params[0])) {
			info(admin, null);
			return;
		}
		
		if (params.length < 2) {
			info(admin, null);
			return;
		}

		int itemId = 0;
		long itemCount = 1;
		byte itemCountIndex = 2;
		Player target = World.getInstance().getPlayer(Util.convertName(params[0]));
		if (target == null) {
			info(admin, "玩家不在线。");
			return;
		}

		String itemString = params[1];
		if (itemString.equals("[item:") && params.length >= 2) {
			// 有些物品链接在ID前有空格
			itemString += params[2];
			if (params.length > 3) {
				itemCountIndex = 3;
			}
		}
		try {
			if (params.length > 2 && (itemCountIndex < params.length)) {
				// 传递了数量参数
				itemCount = Long.parseLong(params[itemCountIndex]);
			}

			Pattern id = Pattern.compile("(?:\\[item:)??(\\d{9})");
			Matcher result = id.matcher(itemString);
			if (result.find()) {
				itemId = Integer.parseInt(result.group(1));
			}

		} catch (NumberFormatException e) {
			info(admin, "传递了无效的数字参数。");
			return;
		}

		if (itemId > 0) {
			if (itemCount > 0) {
				Storage bag = target.getInventory();
				long bagItemCount = bag.getItemCountByItemId(itemId);
				if (bagItemCount >= 1) {
					if (itemCount <= bagItemCount) {
						bag.decreaseByItemId(itemId, itemCount);
						PacketSendUtility.sendMessage(admin, "成功从" + target.getName() + "的物品栏中移除了" + itemCount + "个 [item:" + itemId + "]。");
						PacketSendUtility.sendMessage(target, "管理员从您的物品栏中移除了" + itemCount + "个 [item:" + itemId + "]。");
					} else {
						info(admin, "玩家只有" + bagItemCount + "个该物品。");
					}
				} else {
					info(admin, "玩家没有该物品。");
				}
			} else {
				info(admin, "物品数量无效。");
			}
		} else {
			info(admin, "物品ID无效。");
		}
	}

	@Override
	public void info(Player player, String message) {
		if (message != null && !message.isEmpty()) {
			PacketSendUtility.sendMessage(player, message);
		}
		PacketSendUtility.sendMessage(player, "语法：//remove <玩家名> <物品ID|物品@链接> [数量]");
	}
}