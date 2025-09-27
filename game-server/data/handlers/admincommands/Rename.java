package admincommands;

import com.aionemu.gameserver.configs.main.NameConfig;
import com.aionemu.gameserver.dao.OldNamesDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.clientpackets.CM_APPEARANCE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.NameRestrictionService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author xTz
 */
public class Rename extends AdminCommand {

  public Rename() {
    super("rename", "更改玩家的名称。");

    // @formatter:off
    setSyntaxInfo(
      "<新名称> - 重命名您的目标玩家。",
      "<玩家名称> <新名称> [f] - 重命名指定玩家（f = 强制重命名，忽略保留名称）。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length < 1 || (params.length > 0 && "help".equals(params[0]))) {
      sendInfo(admin);
      return;
    }

    String oldName = params.length == 1 ? null : Util.convertName(params[0]);
    String newName = Util.convertName(params.length == 1 ? params[0] : params[1]);
    Player renamed = params.length == 1 && admin.getTarget() instanceof Player player ? player : World.getInstance().getPlayer(oldName);
    PlayerCommonData renamedCommonData = renamed == null ? PlayerService.getOrLoadPlayerCommonData(oldName) : renamed.getCommonData();

    if (renamedCommonData == null) {
      PacketSendUtility.sendPacket(admin, oldName == null ? SM_SYSTEM_MESSAGE.STR_INVALID_TARGET() : SM_SYSTEM_MESSAGE.STR_NO_USER_NAMED(oldName));
      return;
    } else {
      oldName = renamedCommonData.getName();
    }
    if (!NameRestrictionService.isValidName(newName) || NameRestrictionService.isForbidden(newName)) {
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_MSG_EDIT_CHAR_NAME_ERROR_WRONG_INPUT());
      return;
    }
    int nameReservationDurationDays = params.length >= 3 && params[2].equalsIgnoreCase("f") ? 0 : NameConfig.RESERVE_OLD_NAME_DAYS;
    if (PlayerService.isNameUsedOrReserved(oldName, newName, nameReservationDurationDays)) {
      PacketSendUtility.sendPacket(admin, SM_SYSTEM_MESSAGE.STR_MSG_EDIT_CHAR_NAME_ALREADY_EXIST());
      return;
    }

    OldNamesDAO.insertNames(renamedCommonData.getPlayerObjId(), oldName, newName);
    renamedCommonData.setName(newName);
    PlayerDAO.storePlayerName(renamedCommonData);
    if (renamed != null)
      CM_APPEARANCE.onPlayerNameChanged(renamed, oldName);
    sendInfo(admin, oldName + " 已重命名为 " + newName);
  }
}