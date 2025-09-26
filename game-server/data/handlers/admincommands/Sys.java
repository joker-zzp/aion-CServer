package admincommands;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.aionemu.commons.utils.ExitCode;
import com.aionemu.commons.utils.info.SystemInfo;
import com.aionemu.commons.utils.info.VersionInfo;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.ShutdownConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.time.ServerTime;

/**
 * 系统控制命令类 - 允许管理员查看和控制系统环境
 * @author lord_rex
 */
public class Sys extends AdminCommand {

	public Sys() {
		super("sys", "查看和控制系统环境。");

		// @formatter:off
		setSyntaxInfo(
			"help - 显示系统控制命令的帮助信息",
			"<info> - 显示系统一般信息。",
			"<memory> [gc] - 显示内存使用统计信息，可选：执行垃圾回收。",
			"<threadpool> - 显示线程池管理器信息。",
			"<restart|shutdown> [delay] - 在指定延迟秒数后重启或关闭服务器（默认：使用配置中的延迟）。"
		);
		// @formatter:on
	}

	@Override
	public void execute(Player player, String... params) {
		// 处理help参数
		if (params != null && params.length > 0 && "help".equalsIgnoreCase(params[0])) {
			sendInfo(player);
			return;
		}
		
		if (params == null || params.length < 1) {
			sendInfo(player);
			return;
		}

		if ("info".equalsIgnoreCase(params[0])) {
			sendInfo(player, "系统信息时间: " + ServerTime.now().format(DateTimeFormatter.ofPattern("H:mm:ss")));
			sendInfo(player, VersionInfo.commons.toString(GSConfig.TIME_ZONE_ID));
			sendInfo(player, GameServer.versionInfo.toString(GSConfig.TIME_ZONE_ID));
			sendInfo(player, SystemInfo.getSystemInfo());
		} else if ("memory".equalsIgnoreCase(params[0])) {
			if (params.length > 1 && "gc".equalsIgnoreCase(params[1])) {
				long time = System.currentTimeMillis();
				System.gc();
				sendInfo(player, "垃圾回收耗时 " + (System.currentTimeMillis() - time) + " 毫秒");
			}
			sendInfo(player, SystemInfo.getMemoryInfo());
		} else if ("threadpool".equalsIgnoreCase(params[0])) {
			List<String> stats = ThreadPoolManager.getInstance().getStats();
			for (String stat : stats) {
				sendInfo(player, stat.replaceAll("\t", ""));
			}
		} else if ("shutdown".equalsIgnoreCase(params[0])) {
			initShutdown(ExitCode.NORMAL, params.length == 1 ? null : params[1]);
		} else if ("restart".equalsIgnoreCase(params[0])) {
			initShutdown(ExitCode.RESTART, params.length == 1 ? null : params[1]);
		} else {
			sendInfo(player);
		}
	}

	private void initShutdown(int exitCode, String delay) {
		int delaySeconds = delay == null ? ShutdownConfig.DELAY : Integer.parseInt(delay);
		GameServer.initShutdown(exitCode, delaySeconds);
	}

}