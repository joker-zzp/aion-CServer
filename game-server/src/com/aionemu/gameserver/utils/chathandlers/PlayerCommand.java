package com.aionemu.gameserver.utils.chathandlers;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CommandsAccessService;

/**
 * @author synchro2, Neon
 */
public abstract class PlayerCommand extends ChatCommand {

  public final static String PREFIX = ".";

  /**
   * Registers a new player command.
   * 
   * @param alias
   *          the command name
   * @param description
   *          description what the command does
   */
  public PlayerCommand(String alias, String description) {
    super(PREFIX, alias, description);
  }

  @Override
  public boolean validateAccess(Player player) {
    return player.hasPermission(getLevel()) || CommandsAccessService.hasAccess(player.getObjectId(), getAlias());
  }

  @Override
  boolean process(Player player, String... params) {

    if (!validateAccess(player)) {
      if (player.isStaff()) {
        sendInfo(player, "<你需要 " + getLevel() + " 级或以上权限才能使用 " + getAliasWithPrefix() + ">");
        return true;
      }
      // return false so chat will send entered text (this way you can't guess commands without rights)
      return false;
    }

    if (!run(player, params))
      sendInfo(player, "<执行命令时出错>");

    return true;
  }
}
