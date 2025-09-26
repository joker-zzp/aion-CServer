// 导出业务解析器模块
pub mod config_based_parser;
pub mod skill_parser;
pub mod item_parser;
pub mod npc_parser;

// 重新导出解析器结构体，方便外部使用
pub use skill_parser::SkillTemplateParser;
pub use item_parser::ItemTemplateParser;
pub use npc_parser::NpcTemplateParser;
pub use config_based_parser::ConfigBasedParser;