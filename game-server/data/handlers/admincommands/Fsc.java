package admincommands;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUSTOM_PACKET.PacketElementType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 此服务器命令用于从服务器创建并发送自定义数据包到客户端。用于开发目的。<br>
 * <b>命令名称: //fsc</b></br> <b>参数:</b>
 * <ul>
 * <li>packet id (一个字节) - 可以是十进制格式 (例如 227), 也可以是十六进制格式 (例如 0xE3)</li>
 * <li>包格式字符串 - 包含以下字母的字符串: d (代表 writeD()), h (代表 writeH()), c (代表 writeC()), f
 * (代表 writeF()), e (代表 writeDF()), q (代表 writeQ()), s (代表 writeS())</li>
 * <li>数据列表 - 这里是对应于适当格式部分的所有数据。</li>
 * </ul>
 * 示例:<br>
 * //fsc 0xD8 cdds 8 50 80 someText - 将发送ID为0xD8的数据包(子ID将自动添加)，然后发送一个字节 - 8，接着是两个整数
 * -50和80，最后是一个字符串 - someText
 * 
 * @author Luno
 */
public class Fsc extends AdminCommand {

	public Fsc() {
		super("fsc");
	}

	@Override
	public void execute(Player player, String... params) {
		if (params.length < 3) {
			PacketSendUtility.sendMessage(player, "//fsc命令参数数量不正确");
			return;
		}

		int id = Integer.decode(params[0]);
		String format = params[1];

		SM_CUSTOM_PACKET packet = new SM_CUSTOM_PACKET(id);

		int i = 0;
		for (char c : format.toCharArray()) {
			packet.addElement(PacketElementType.getByCode(c), params[i + 2]);
			i++;
		}
		PacketSendUtility.sendPacket(player, packet);
	}

	@Override
	public void info(Player player, String message) {
		PacketSendUtility.sendMessage(player, "//fsc命令参数数量不正确");
	}
}