package admincommands;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author Divinity
 */
public class Announcements extends AdminCommand {

  public Announcements() {
    super("announcements", "管理自动公告");

    // @formatter:off
    setSyntaxInfo(
      "<list> - 显示所有公告及其ID",
      "<reload> - 从数据库重新加载所有公告",
      "<add> <天族|魔族|全部> <聊天类型> <延迟> <消息> - 添加指定消息(延迟单位为秒,聊天类型可以是system、white、orange、shout或yellow)",
      "<delete> <id> - 删除指定ID的公告"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player player, String... params) {
    if (params.length == 0) {
      sendInfo(player);
      return;
    }

    if (params[0].equals("list")) {
      Collection<Announcement> announcements = AnnouncementService.getInstance().getAnnouncements();
      String msg;
      if (announcements.isEmpty()) {
        msg = "当前没有活动的公告";
      } else {
        msg = "公告列表:";
        for (Announcement announce : announcements) {
          msg += "\nID: " + announce.getId() + " (chat type: " + announce.getType() + ", delay: " + announce.getDelay() + "s";
          if (announce.getFaction() != null)
            msg += ", faction: " + announce.getFaction();
          msg += ")\n\t\"" + announce.getAnnounce() + "\"";
        }
      }
      sendInfo(player, msg);
    } else if (params[0].equals("reload")) {
      AnnouncementService.getInstance().reload();
      sendInfo(player, "已重新加载 " + AnnouncementService.getInstance().getAnnouncements().size() + " 条公告");
    } else if (params[0].equals("add")) {
      if (params.length < 4) {
        sendInfo(player);
        return;
      }

      String faction = params[1].toUpperCase();
      if (!Arrays.asList("ELYOS", "ASMODIANS", "ALL").contains(faction)) {
        sendInfo(player, "请指定有效的种族参数");
        return;
      }

      String chatType = params[2].toUpperCase();
      if (!Arrays.asList("SYSTEM", "WHITE", "ORANGE", "SHOUT", "YELLOW").contains(chatType)) {
        sendInfo(player, "请指定有效的聊天类型参数");
        return;
      }

      int delay;
      try {
        delay = Integer.parseInt(params[3]);
        if (delay < 300)
          throw new IllegalArgumentException("延迟必须至少为300秒(5分钟)");
      } catch (IllegalArgumentException e) {
        sendInfo(player, e instanceof NumberFormatException ? "延迟必须以秒为单位指定" : e.getMessage());
        return;
      }

      String message = StringEscapeUtils.unescapeJava(StringUtils.join(params, ' ', 4, params.length));
      if (message.isEmpty()) {
        sendInfo(player, "消息不能为空");
        return;
      }

      if (AnnouncementService.getInstance().addAnnouncement(message, faction, chatType, delay))
          sendInfo(player, "公告已成功创建");
        else
          sendInfo(player, "公告创建失败");
    } else if (params[0].equals("delete")) {
      if (params.length < 2) {
        sendInfo(player, "请指定要删除的公告ID");
        return;
      }

      int id;

      try {
        id = Integer.parseInt(params[1]);
      } catch (NumberFormatException e) {
        sendInfo(player, "无效的公告ID");
        return;
      }

      // Delete the announcement from the database
      if (AnnouncementService.getInstance().delAnnouncement(id))
          sendInfo(player, "公告已成功删除");
        else
          sendInfo(player, "公告删除失败");
    } else {
      sendInfo(player);
    }
  }

}