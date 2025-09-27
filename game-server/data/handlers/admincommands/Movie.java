package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author d3v1an
 */
public class Movie extends AdminCommand {

  public Movie() {
    super("movie");

    // @formatter:off
    setSyntaxInfo(
      "<cutsceneId> - 播放过场动画 (根据当前地图渲染效果可能会有差异)",
      "m <movieId> - 播放指定ID的电影过场动画"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player player, String... params) {
    if (params.length == 0) {
      sendInfo(player);
      return;
    }
    boolean isCutsceneMovie = "m".equalsIgnoreCase(params[0]);
    int cutsceneId = Integer.parseInt(params[isCutsceneMovie ? 1 : 0]);
    PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(isCutsceneMovie, 0, 0, cutsceneId, true));
  }
}
