package admincommands;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author ginho1, Damon
 */
public class AddEmotion extends AdminCommand {

  public AddEmotion() {
    super("addemotion", "添加表情");
  }

  @Override
  public void execute(Player admin, String... params) {

    long expireMinutes = 0;
    int emotionId = 0;
    VisibleObject target = null;
    Player finalTarget = null;

    if ((params.length < 1) || (params.length > 2)) {
      PacketSendUtility.sendMessage(admin, "语法 //addemotion <表情ID [过期时间] || html>\nhtml 显示带有名称的HTML页面");
      return;
    }

    try {
      emotionId = Integer.parseInt(params[0]);
      if (params.length == 2)
        expireMinutes = Long.parseLong(params[1]);
    } catch (NumberFormatException ex) {
      if (params[0].equalsIgnoreCase("html"))
        HTMLService.showHTML(admin, HTMLCache.getInstance().getHTML("emote.xhtml"));
      return;
    }

    if (emotionId < 1 || (emotionId > 35 && emotionId < 64) || emotionId > 129) {
      PacketSendUtility.sendMessage(admin, "无效的表情ID 必须在区间 [1-35]U[64-129] 内");
      return;
    }

    target = admin.getTarget();

    if (target == null)
      finalTarget = admin;
    else if (target instanceof Player)
      finalTarget = (Player) target;
    else
      return;

    if (finalTarget.getEmotions().contains(emotionId)) {
      PacketSendUtility.sendMessage(admin, "目标已经拥有此表情");
      return;
    }

    if (params.length == 2) {
      finalTarget.getEmotions().add(emotionId, (int) ((System.currentTimeMillis() / 1000) + expireMinutes * 60), true);
    } else {
      finalTarget.getEmotions().add(emotionId, 0, true);
    }
  }
}