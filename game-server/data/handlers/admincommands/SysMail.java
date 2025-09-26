package admincommands;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.LetterType;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.services.mail.MailFormatter;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 系统邮件命令类 - 允许管理员向玩家发送系统邮件，可包含物品和金币
 * 
 * @author xTz
 */
public class SysMail extends AdminCommand {

	/**
	 * 创建系统邮件命令
	 */
	public SysMail() {
		super("sysmail");
	}

	enum RecipientType {

		ELYOS,
		ASMO,
		ALL,
		PLAYER;

		public boolean isAllowed(Race race) {
			switch (this) {
				case ELYOS:
					return race == Race.ELYOS;
				case ASMO:
					return race == Race.ASMODIANS;
				case ALL:
					return race == Race.ELYOS || race == Race.ASMODIANS;
				default:
					return false;
			}
		}
	}

	@Override
	public void execute(Player admin, String... params) {
		// 支持help参数
		if (params.length == 1 && "help".equalsIgnoreCase(params[0])) {
			info(admin, null);
			return;
		}
		
		if (params.length < 5) {
			info(admin, null);
			return;
		}

		String[] paramValues = new String[params.length];
		System.arraycopy(params, 0, paramValues, 0, params.length);

		RecipientType recipientType = null;
		String sender = null;
		String recipient = null;

		if (paramValues[0].startsWith("$$") || paramValues[0].startsWith("%")) {
			if (params.length < 6) {
				info(admin, null);
				return;
			}
			sender = paramValues[0];
			paramValues = new String[params.length - 1];
			System.arraycopy(params, 1, paramValues, 0, params.length - 1);
		} else {
			sender = "Admin";
		}

		if (paramValues[0].startsWith("@")) {
			if ("@all".startsWith(paramValues[0]))
				recipientType = RecipientType.ALL;
			else if ("@elyos".startsWith(paramValues[0]))
				recipientType = RecipientType.ELYOS;
			else if ("@asmodians".startsWith(paramValues[0]))
				recipientType = RecipientType.ASMO;
			else {
						PacketSendUtility.sendMessage(admin, "收件人必须是玩家名称、@all、@elyos或@asmodians。");
						return;
					}
		} else {
			recipientType = RecipientType.PLAYER;
			recipient = Util.convertName(paramValues[0]);
		}

		int item = 0, count = 0, kinah = 0;
		LetterType letterType;

		try {
			item = Integer.parseInt(paramValues[2]);
			count = Integer.parseInt(paramValues[3]);
			kinah = Integer.parseInt(paramValues[4]);
			letterType = LetterType.getLetterTypeById(Integer.parseInt(paramValues[1]));
		} catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "<普通|乌云贸易团|快递> <物品|数量|基纳>的值必须是整数。");
			return;
		}

		if (letterType == LetterType.BLACKCLOUD)
			sender = "$$CASH_ITEM_MAIL";

		boolean express = checkExpress(admin, item, count, kinah, recipient, recipientType, letterType);
		if (!express)
			return;

		if (item <= 0)
			item = 0;

		if (count <= 0)
			count = -1;

		String title = "System Mail";
		String message = " ";

		if (paramValues.length > 5) {
			String[] words = new String[paramValues.length - 5];
			System.arraycopy(paramValues, 5, words, 0, words.length);
			String[] outText = new String[1];
			int wordCount = extractText(words, outText);
			if (wordCount > 0) {
				title = outText[0];
				String[] msgWords = new String[words.length - wordCount];
				System.arraycopy(words, wordCount, msgWords, 0, msgWords.length);
				wordCount = extractText(msgWords, outText);
				if (wordCount > 0)
					message = outText[0];
			}
		}

		if (recipientType == RecipientType.PLAYER) {
			if (letterType == LetterType.BLACKCLOUD)
				MailFormatter.sendBlackCloudMail(recipient, item, count);
			else
				SystemMailService.sendMail(sender, recipient, title, message, item, count, kinah, letterType);
		} else {
			for (Player player : World.getInstance().getAllPlayers()) {
				if (recipientType.isAllowed(player.getRace())) {
					if (letterType == LetterType.BLACKCLOUD)
						MailFormatter.sendBlackCloudMail(player.getName(), item, count);
					else
						SystemMailService.sendMail(sender, player.getName(), title, message, item, count, kinah, letterType);
				}
			}
		}

		if (item != 0) {
			PacketSendUtility.sendMessage(admin, "You send to " + recipientType + (recipientType == RecipientType.PLAYER ? " " + recipient : "") + "\n"
				+ "[item:" + item + "] Count:" + count + " Kinah:" + kinah + "\n" + "Letter send successfully.");
		} else if (kinah > 0) {
			PacketSendUtility.sendMessage(admin, "You send to " + recipientType + (recipientType == RecipientType.PLAYER ? " " + recipient : "") + "\n"
				+ " Kinah:" + kinah + "\n" + "Letter send successfully.");
		}
	}

	private int extractText(String[] words, String[] outText) {
		if (words.length == 0 || outText.length == 0)
			return 0;

		if (!words[0].startsWith("|"))
			return 0;

		int wordCount = 1;

		String enclosedText = words[0].substring(1);
		if (enclosedText.endsWith("|")) {
			outText[0] = enclosedText.substring(0, enclosedText.length() - 1);
		} else {
			List<String> titleWords = new ArrayList<>();
			titleWords.add(enclosedText);
			for (; wordCount < words.length; wordCount++) {
				String word = words[wordCount];
				if (word.endsWith("|")) {
					word = word.substring(0, word.length() - 1);
					titleWords.add(word);
					wordCount++;
					break;
				} else
					titleWords.add(word);
			}

			outText[0] = String.join(" ", titleWords);
		}

		return wordCount;
	}

	private static boolean checkExpress(Player admin, int item, int count, int kinah, String recipient, RecipientType recipientType,
		LetterType letterType) {
		boolean shouldExpress = false;

		if (recipientType == null) {
			PacketSendUtility.sendMessage(admin, "请输入收件人类型。\n" + "收件人 = 玩家名称, @all, @elyos或@asmodians");
			return false;
		} else if (recipientType == RecipientType.PLAYER) {
			if (letterType == LetterType.NORMAL) {
				if (!PlayerDAO.isNameUsed(recipient)) {
								PacketSendUtility.sendMessage(admin, "找不到该名称的收件人。");
								return false;
							}
				shouldExpress = true;
			} else if (letterType == LetterType.EXPRESS) {
								if (World.getInstance().getPlayer(recipient) == null) {
									PacketSendUtility.sendMessage(admin, "该收件人不在线。");
									return false;
								}
				shouldExpress = true;
			} else { // Black cloud
				shouldExpress = World.getInstance().getPlayer(recipient) != null;
			}
		} else {
			shouldExpress = letterType != LetterType.NORMAL;
		}

		if (item == 0 && count != 0) {
			PacketSendUtility.sendMessage(admin, "请输入物品ID。");
			return false;
		}

		if (count == 0 && item != 0) {
			PacketSendUtility.sendMessage(admin, "请输入物品数量。");
			return false;
		}

		if (count <= 0 && item <= 0 && kinah <= 0) {
			PacketSendUtility.sendMessage(admin, "参数<物品> <数量> <基纳>不正确。");
			return false;
		}

		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(item);
		if (item != 0) {
			if (itemTemplate == null) {
				PacketSendUtility.sendMessage(admin, "物品ID不正确: " + item);
				return false;
			}
			long maxStackCount = itemTemplate.getMaxStackCount();
			if (count > maxStackCount && maxStackCount != 0) {
					PacketSendUtility.sendMessage(admin, "请输入正确的物品数量。");
					return false;
				}
		}

		if (kinah < 0) {
			PacketSendUtility.sendMessage(admin, "基纳值必须 >= 0。");
			return false;
		} else if (kinah > 0 && letterType == LetterType.BLACKCLOUD) {
			PacketSendUtility.sendMessage(admin, "乌云贸易团邮件不能附加基纳！");
			return false;
		}
		return shouldExpress;
	}

	@Override
	public void info(Player player, String message) {
		PacketSendUtility.sendMessage(player, "未检测到参数。\n"
				+ "请使用 //sysmail [帮助] [%|$$<发件人>] <收件人> <普通|乌云贸易团|快递> <物品ID> <数量> <基纳> [|标题|] [|消息|]\n"
				+ "发件人名称必须以%或$$开头，可省略。\n" + "普通邮件类型为0，快递邮件类型为1，乌云贸易团类型为2。\n"
				+ "如果参数(物品ID, 数量) = 0，则不会发送物品\n" + "如果参数(基纳) = 0，则不发送基纳\n"
				+ "收件人 = 玩家名称, @all, @elyos或@asmodians\n" + "可选的标题和消息必须用竖线字符括起来");
	}

}