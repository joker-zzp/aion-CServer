package admincommands;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * 速度调整命令类 - 允许管理员设置角色的移动速度
 * @author ATracer, Neon
 */
public class Speed extends AdminCommand implements StatOwner {

	public Speed() {
		super("speed", "设置你的移动速度。");

		setSyntaxInfo("<0-100> - 设置你的移动速度为指定值(0表示重置).");
	}

	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0 || "help".equals(params[0])) {
			sendInfo(admin);
			return;
		}

		float parameter = 0;
		try {
			parameter = Float.parseFloat(params[0]);
			if (parameter < 0 || parameter > 100) {
				throw new IllegalArgumentException("速度值必须在0到100之间。");
			}
		} catch (IllegalArgumentException e) {
			sendInfo(admin, e.getClass() == IllegalArgumentException.class ? e.getMessage() : null); // 默认信息用于数字格式化异常
			return;
		}

		admin.getGameStats().endEffect(this);
		if (parameter == 0) {
			sendInfo(admin, "已恢复你的标准移动速度。");
			return;
		}

		List<IStatFunction> functions = new ArrayList<>();
		functions.add(new SpeedFunction(StatEnum.SPEED, parameter));
		functions.add(new SpeedFunction(StatEnum.FLY_SPEED, parameter));
		admin.getGameStats().addEffect(this, functions);
		sendInfo(admin, "你的移动速度已设置为 " + parameter);
	}

	class SpeedFunction extends StatFunction {

		private int speed;

		SpeedFunction(StatEnum stat, float speed) {
			this.stat = stat;
			this.speed = (int) (speed * 1000);
		}

		@Override
		public void apply(Stat2 otherStat, CalculationType... calculationTypes) {
			otherStat.setBase(speed);
			otherStat.setBaseRate(1);
			otherStat.setBonus(0);
		}

		@Override
		public int getPriority() {
			return 120;
		}
	}
}