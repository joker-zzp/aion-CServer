package admincommands;

import java.awt.Color;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.pet.PetFunction;
import com.aionemu.gameserver.model.templates.pet.PetTemplate;
import com.aionemu.gameserver.services.toypet.PetAdoptionService;
import com.aionemu.gameserver.utils.ChatUtil;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * @author ATracer, Neon
 */
public class Pet extends AdminCommand {

  public Pet() {
    super("pet", "添加或移除宠物。");

    // @formatter:off
    setSyntaxInfo(
      "<list> - 列出所有可用的宠物ID。",
      "<add> <宠物ID> <名称> - 添加指定ID的宠物并为其命名。",
      "<del> <宠物ID> - 删除指定ID的宠物。"
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    String action = params[0];
    if (action.equalsIgnoreCase("list")) {
      StringBuilder sb = new StringBuilder("宠物列表：");
      DataManager.PET_DATA.getPetIds().stream().sorted().forEach(id -> {
        PetTemplate template = DataManager.PET_DATA.getPetTemplate(id);
        sb.append('\n');
        sb.append(template.getTemplateId());
        sb.append(" - ");
        sb.append(ChatUtil.color(StringUtils.capitalize(template.getName()), Color.WHITE));
        sb.append("\n\t功能: ");
        Iterator<PetFunction> iter = template.getPetFunctions().iterator();
        while (iter.hasNext())
          sb.append(iter.next().getPetFunctionType() + (iter.hasNext() ? ", " : ""));
      });
      sendInfo(admin, sb.toString());
    } else {
      int petId;

      try {
        petId = Integer.parseInt(params[1]);
        if (DataManager.PET_DATA.getPetTemplate(petId) == null)
          throw new IllegalArgumentException();
      } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
        sendInfo(admin, e instanceof ArrayIndexOutOfBoundsException ? "您必须指定宠物ID。" : "宠物ID无效。");
        return;
      }

      if (action.equalsIgnoreCase("add")) {
        if (params.length != 3) {
        sendInfo(admin, "您必须为宠物指定一个名称。");
        return;
      }
        PetAdoptionService.addPet(admin, petId, params[2], 0, 0);
      } else if (action.equalsIgnoreCase("del")) {
        PetAdoptionService.surrenderPet(admin, petId);
      }
    }
  }
}