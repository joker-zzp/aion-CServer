package admincommands;

import org.apache.commons.lang3.math.NumberUtils;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.world.WeatherEntry;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 天气控制命令类 - 允许管理员查看或更改游戏中的天气
 * @author Kwazar
 */
public class Weather extends AdminCommand {

  public Weather() {
    super("weather", "查看/更改游戏中的天气");

    // @formatter:off
    setSyntaxInfo(
      "<help> - 显示天气命令的帮助信息。",
      "<info> - 显示当前区域的天气信息。",
      "<next> - 触发当前地图的自然天气变化。",
      "<set> <code> - 根据天气代码（0-12之间）更改当前地图的天气。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    switch (params[0].toLowerCase()) {
      case "help":
        sendInfo(admin);
        return;
      case "info":
        for (ZoneInstance regionZone : admin.findZones()) {
          if (regionZone.getZoneTemplate().getZoneType() == ZoneClassName.WEATHER) {
            int weatherZoneId = DataManager.ZONE_DATA.getWeatherZoneId(regionZone.getZoneTemplate());
            WeatherEntry weatherEntry = WeatherService.getInstance().getWeatherEntry(admin.getWorldId(), weatherZoneId);
            if (weatherEntry != null) {
              String info = "区域 " + regionZone.getZoneTemplate().getXmlName() + " 的天气信息:";
              if (weatherEntry == WeatherEntry.NONE) {
                info += "\n\t代码: " + weatherEntry.getCode() + " (无天气)";
              } else {
                if (weatherEntry.getZoneId() > 0)
                  info += "\n\t区域ID: " + weatherEntry.getZoneId();
                if (weatherEntry.getWeatherName() != null)
                  info += "\n\t名称: " + weatherEntry.getWeatherName();
                info += "\n\t代码: " + weatherEntry.getCode();
              }
              sendInfo(admin, info);
              return;
            }
          }
        }
        sendInfo(admin, "该区域未定义天气.");
        return;
      case "set":
      case "next":
        int weatherCode;
        if (params[0].equalsIgnoreCase("next")) {
          if (params.length != 1) {
            sendInfo(admin);
            return;
          }
          weatherCode = -1;
        } else {
          weatherCode = NumberUtils.toInt(params[1], -1);
          if (weatherCode < 0 || weatherCode > 12) {
            sendInfo(admin, "天气代码必须在0到12之间.");
            return;
          }
        }

        if (WeatherService.getInstance().changeWeather(admin.getWorldId(), weatherCode)) {
          String weatherName = WeatherService.getInstance().findWeatherEntry(admin).getWeatherName();
          sendInfo(admin, "天气已更改" + (weatherName == null ? "." : "为 " + weatherName + "."));
        } else {
          sendInfo(admin, "该区域未定义天气.");
        }
        return;
    }
  }
}