package admincommands;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.aionemu.gameserver.ai.AIEngine;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dataholders.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.templates.event.EventTemplate;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;
import com.aionemu.gameserver.services.event.EventService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.utils.xml.JAXBUtil;
import com.aionemu.gameserver.utils.xml.XmlUtil;

/**
 * @author MrPoke, Neon
 */
public class Reload extends AdminCommand {

  public Reload() {
    super("reload", "重新加载模板或处理器（静态数据）。");

    // @formatter:off
    setSyntaxInfo(
      "<config> - 重新加载所有配置设置。",
      "<commands|ai> - 重新加载指定的处理器。",
      "<quests> - 重新加载任务模板和处理器。",
      "<skills|npcskills> - 重新加载指定的技能模板。",
      "<events> - 重新加载事件模板并（重新）启动事件。",
      "<arcade> - 重新加载升级街机奖励物品列表。",
      "<decomposables> - 重新加载物品包内容。",
      "<items|customdrops|gameshop> - 重新加载指定的数据。" 
    );
    // @formatter:on
  }

  @Override
  public void execute(Player admin, String... params) {
    if (params.length == 0) {
      sendInfo(admin);
      return;
    }

    if ("help".equalsIgnoreCase(params[0])) {
      sendInfo(admin);
      return;
    }
    if (params[0].equalsIgnoreCase("quests")) {
      File xml = new File("./data/static_data/quest_data/quest_data.xml");
      DataManager.QUEST_DATA = JAXBUtil.deserialize(xml, QuestsData.class, "./data/static_data/static_data.xsd");
      List<XMLQuest> templates = new ArrayList<>();
      Collection<File> files = XmlUtil.listFiles("./data/static_data/quest_script_data", true);
      JAXBUtil.deserialize(files, XMLQuests.class, "./data/static_data/static_data.xsd").forEach(e -> templates.addAll(e.getAllQuests()));
      DataManager.XML_QUESTS.setData(templates);
      QuestEngine.getInstance().reload();
      sendInfo(admin, DataManager.QUEST_DATA.size() + " 个任务模板已加载（" + QuestEngine.getInstance().getQuestHandlerCount() + " 个处理器）。");
    } else if (params[0].equalsIgnoreCase("skills")) {
      File xml = new File("./data/static_data/skills/skill_templates.xml");
      DataManager.SKILL_DATA = JAXBUtil.deserialize(xml, SkillData.class, "./data/static_data/static_data.xsd");
      sendInfo(admin, DataManager.SKILL_DATA.size() + " 个技能已加载。");
    } else if (params[0].equalsIgnoreCase("npcskills")) {
      List<NpcSkillTemplates> templates = new ArrayList<>();
      Collection<File> files = XmlUtil.listFiles("./data/static_data/npc_skills", true);
      JAXBUtil.deserialize(files, NpcSkillData.class, "./data/static_data/static_data.xsd")
        .forEach(e -> templates.addAll(e.getAllNpcSkillTemplates()));
      DataManager.NPC_SKILL_DATA.setNpcSkillTemplates(templates);
      sendInfo(admin, DataManager.NPC_SKILL_DATA.size() + " 个NPC技能已加载。");
    } else if (params[0].equalsIgnoreCase("items")) {
      File xml = new File("./data/static_data/items/item_templates.xml");
      DataManager.ITEM_DATA = JAXBUtil.deserialize(xml, ItemData.class, "./data/static_data/static_data.xsd");
      DataManager.ITEM_DATA.cleanup();
      sendInfo(admin, DataManager.ITEM_DATA.size() + " 个物品模板已加载。");
    } else if (params[0].equalsIgnoreCase("ai")) {
      AIEngine.getInstance().reload();
      sendInfo(admin, "AI已成功重新加载！");
    } else if (params[0].equalsIgnoreCase("commands")) {
      ChatProcessor.getInstance().reload();
      sendInfo(admin, "聊天命令已成功重新加载！");
    } else if (params[0].equalsIgnoreCase("config")) {
      Config.load();
      sendInfo(admin, "配置已成功重新加载！");
    } else if (params[0].equalsIgnoreCase("customdrops")) {
      File xml = new File("./data/static_data/custom_drop/custom_drop.xml");
      DataManager.CUSTOM_NPC_DROP = JAXBUtil.deserialize(xml, CustomDrop.class, "./data/static_data/static_data.xsd");
      sendInfo(admin, DataManager.CUSTOM_NPC_DROP.size() + " 个自定义掉落已加载。");
    } else if (params[0].equalsIgnoreCase("gameshop")) {
      InGameShopEn.getInstance().reload();
      sendInfo(admin, "游戏商城已成功重新加载！");
    } else if (params[0].equalsIgnoreCase("events")) {
      List<EventTemplate> templates = new ArrayList<>();
      Collection<File> files = XmlUtil.listFiles("./data/static_data/events/timed_events", true);
      JAXBUtil.deserialize(files, EventData.class, "./data/static_data/static_data.xsd").forEach(e -> templates.addAll(e.getEvents()));
      EventService.getInstance().stop();
      DataManager.EVENT_DATA.setEvents(templates);
      EventService.getInstance().start();
      sendInfo(admin, DataManager.EVENT_DATA.size() + " 个事件已加载。");
    } else if (params[0].equalsIgnoreCase("arcade")) {
      File xml = new File("./data/static_data/events/arcadelist.xml");
      DataManager.UPGRADE_ARCADE_DATA = JAXBUtil.deserialize(xml, UpgradeArcadeData.class, "./data/static_data/static_data.xsd");
      long rewards = DataManager.UPGRADE_ARCADE_DATA.getRewards().stream().mapToLong(l -> l.getArcadeRewardItems().size()).sum();
      sendInfo(admin, rewards + " 个升级街机奖励已加载。");
    } else if (params[0].equalsIgnoreCase("decomposables")) {
      File xml = new File("./data/static_data/decomposable_items/decomposable_items.xml");
      DataManager.DECOMPOSABLE_ITEMS_DATA = JAXBUtil.deserialize(xml, DecomposableItemsData.class, "./data/static_data/decomposable_items/decomposable_items.xsd");
      sendInfo(admin, DataManager.DECOMPOSABLE_ITEMS_DATA.size() + " 个物品包已重新加载。");
    } else
      sendInfo(admin);
  }
}