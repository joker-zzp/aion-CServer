// 配置模块
pub mod schema_config;

// 重新导出配置相关结构体，方便外部使用
pub use schema_config::SchemaConfigManager;
pub use schema_config::{TableConfig, FieldConfig, SpecialFieldConfig, SchemaConfigs};