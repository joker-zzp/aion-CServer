package com.aionemu.gameserver.utils.chathandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CommandsAccessService;

/**
 * @author synchro2, Neon
 */
public abstract class AdminCommand extends ChatCommand {

	// 管理员命令审核日志记录器
	private static final Logger log = LoggerFactory.getLogger("ADMINAUDIT_LOG");
	// 管理员命令前缀标识
	public final static String PREFIX = "//";

	// 仅用于向后兼容 TODO：当所有命令更新后移除
	public AdminCommand(String alias) {
		this(alias, "");
	}

	/**
	 * 注册一个新的管理员命令。
	 * 
	 * @param alias
	 *          命令名称
	 * @param description
	 *          命令功能描述
	 */
	public AdminCommand(String alias, String description) {
		super(PREFIX, alias, description);
	}

	/**
	 * 验证玩家是否有权限使用此命令
	 * @param player 执行命令的玩家对象
	 * @return 如果玩家有权限返回true，否则返回false
	 */	
	@Override
	public boolean validateAccess(Player player) {
		// 检查玩家是否有足够的权限等级，或者通过CommandsAccessService服务是否有特定命令的访问权限
		return player.hasAccess(getLevel()) || CommandsAccessService.hasAccess(player.getObjectId(), getAlias());
	}

	/**
	 * 处理管理员命令的执行流程
	 * @param player 执行命令的玩家
	 * @param params 命令参数列表
	 * @return 命令处理结果
	 */
	@Override
	boolean process(Player player, String... params) {
		// 首先验证玩家权限
		if (!validateAccess(player)) {
			// 如果是Staff成员但权限不足，提示需要的权限等级
			if (player.isStaff()) {
				sendInfo(player, "你需要权限等级 " + getLevel() + " 或更高才能使用 " + getAliasWithPrefix() + ">" );
				return true;
			}
			// 对于非Staff成员，返回false表示不处理该命令，这样聊天系统会原样发送输入文本（防止无权限玩家猜测命令）
			return false;
		}

		// 如果配置启用了管理员命令日志记录，则记录命令执行信息
		if (LoggingConfig.LOG_GMAUDIT)
			log.info(
				"[Admin Command] > [玩家: " + player.getName() + "]" + (player.getTarget() != null ? "[目标: " + player.getTarget().getName() + "]" : "")
					+ ": " + getAliasWithPrefix() + " " + String.join(" ", params));

		// 执行具体的命令逻辑（由子类实现run方法），如果执行失败则发送错误信息
		if (!run(player, params))
			sendInfo(player, "执行命令时出错.");

		// 管理员命令总是返回true表示已处理
		return true;
	}
}
