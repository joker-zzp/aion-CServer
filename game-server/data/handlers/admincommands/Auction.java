package admincommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.collections.Predicates;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * @author Rolandas, Luzien, Neon
 */
public class Auction extends AdminCommand {

  public Auction() {
     super("auction", "添加或移除房屋拍卖");

    // @formatter:off
    setSyntaxInfo(
      "<房屋地址> [起始价格] - 拍卖指定房屋",
      "<区域> <房屋类型> <数量> [起始价格] - 拍卖指定区域内指定类型的空闲房屋",
      "asmo|ely <房屋类型> <数量> [起始价格] - 拍卖空闲的魔族或天族指定类型的房屋",
      "end <房屋地址|区域> - 结束指定房屋(们)的拍卖，将所有权转让给最高出价者",
      "cancel <房屋地址|区域> - 取消指定房屋(们)的拍卖",
      "区域: 来自zones xml文件的区域名称",
      "房屋类型: house(房屋), mansion(豪宅), estate(庄园), palace(宫殿)",
      "如果未指定起始价格，则将使用模板中的默认价格"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    boolean isCancel = "cancel".equalsIgnoreCase(params[0]);
    if ("end".equalsIgnoreCase(params[0]) || isCancel) {
      if (params.length < 2) {
        sendInfo(admin);
        return;
      }

      boolean isHouseAddress = params[1].matches("\\d+");
      List<House> houses;
      if (isHouseAddress) {
        House house = HousingService.getInstance().getHouseByAddress(Integer.parseInt(params[1]));
        if (house == null) {
          sendInfo(admin, "无效的房屋地址");
          return;
        }
        houses = Collections.singletonList(house);
      } else {
        houses = findHousesInZone(admin, params[1], Predicates.alwaysTrue());
        if (houses == null)
          return;
      }

      int removedHouses = 0;
      for (House house : houses) {
        if (!isCancel && HousingBidService.getInstance().endAuction(house.getObjectId())
          || isCancel && HousingBidService.getInstance().cancelAuction(house)) {
          sendInfo(admin,
            (isCancel ? "已取消" : "已结束") + "房屋" + house.getHouseType().name().toLowerCase() + " " + house.getAddress().getId() + "的拍卖");
          removedHouses++;
        }
      }
      if (removedHouses == 0)
          sendInfo(admin, isHouseAddress ? "该房屋不在出售中" : "该区域内没有出售中的房屋");
    } else if (params[0].matches("\\d+")) {
      int address = Integer.parseInt(params[0]);
      House house = HousingService.getInstance().getHouseByAddress(address);
      if (house == null) {
        sendInfo(admin, "无效的地址");
        return;
      }
      if (house.getBids() != null) {
        sendInfo(admin, "地址 " + address + " 已经在拍卖中");
        return;
      }
      long price = params.length < 2 ? house.getDefaultAuctionPrice() : Long.parseLong(params[1]);
      if (price <= 0) {
        sendInfo(admin, "起始价格必须为正数");
        return;
      }
      HousingBidService.getInstance().auction(house, price);
      sendInfo(admin, "地址 " + address + " 已成功拍卖");
    } else if ("add".equals(params[0])) {
      if (params.length < 4 || params.length > 5) {
        sendInfo(admin);
        return;
      }

      HouseType houseType = HouseType.valueOf(params[2].toUpperCase());
      int maxCount = Integer.parseInt(params[3]);
      if (maxCount <= 0) {
        sendInfo(admin, "数量必须为正数");
        return;
      }

      Predicate<House> filter = house -> house.getHouseType() == houseType && house.getBids() == null && house.getOwnerId() == 0;
      List<House> houses;
      if ("asmodians".startsWith(params[1].toLowerCase())) {
        filter = filter.and(house -> house.matchesLandRace(Race.ASMODIANS));
        houses = HousingService.getInstance().getCustomHouses().stream().filter(filter).collect(Collectors.toList());
      } else if ("elyos".startsWith(params[1].toLowerCase())) {
        filter = filter.and(house -> house.matchesLandRace(Race.ELYOS));
        houses = HousingService.getInstance().getCustomHouses().stream().filter(filter).collect(Collectors.toList());
      } else {
        houses = findHousesInZone(admin, params[1], filter);
      }
      if (houses == null)
        return;
      if (houses.isEmpty()) {
        sendInfo(admin, "未找到可拍卖的" + houseType.name().toLowerCase() + "s");
        return;
      }
      long price = params.length < 5 ? houses.get(0).getDefaultAuctionPrice() : Long.parseLong(params[4]);
      if (price <= 0) {
        sendInfo(admin, "起始价格必须为正数");
        return;
      }

      int counter = 0;
      Collections.shuffle(houses);
      for (House house : houses) {
        if (HousingBidService.getInstance().auction(house, price) && ++counter > maxCount)
          break;
      }

      sendInfo(admin, "已拍卖 " + counter + " 个" + houseType.name().toLowerCase() + "s，起始价格为 " + price + " 基纳");
    } else {
      sendInfo(admin);
    }
  }

  private List<House> findHousesInZone(Player admin, String zoneName, Predicate<House> filter) {
    ZoneName zone = ZoneName.get(zoneName);
    if (zone == ZoneName.NONE) {
        sendInfo(admin, "无效的区域名称");
        return null;
      }
    List<House> housesToRemove = new ArrayList<>();
    for (House house : HousingService.getInstance().getCustomHouses()) {
      if (!filter.test(house))
        continue;
      if (house.getPosition().getMapRegion().isInsideZone(zone, house.getX(), house.getY(), house.getZ()))
        housesToRemove.add(house);
    }
    return housesToRemove;
  }
}