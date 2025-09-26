use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs::File;
use std::io::Read;
use std::path::Path;

use crate::utils::db_utils::{TableSchema, TableField};

// 字段配置定义
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct FieldConfig {
    pub name: String,
    pub sql_type: String,
    pub constraints: Vec<String>,
    pub default_value: Option<String>,
    pub is_nullable: Option<bool>,
}

// 表结构配置定义
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct TableConfig {
    pub name: String,
    pub fields: Vec<FieldConfig>,
    pub primary_key: Option<String>,
    pub id_field: String,
    // 特殊处理字段配置
    pub special_fields: Option<HashMap<String, SpecialFieldConfig>>,
}

// 特殊字段配置
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct SpecialFieldConfig {
    pub translate: Option<bool>,
    pub source_attribute: Option<String>,
    pub target_field: Option<String>,
}

// 配置映射类型
pub type SchemaConfigs = HashMap<String, TableConfig>;

// 配置管理器 - 负责加载和解析配置文件
pub struct SchemaConfigManager;

impl SchemaConfigManager {
    // 从YAML文件加载配置
    pub fn load_from_yaml<P: AsRef<Path>>(file_path: P) -> Result<SchemaConfigs, anyhow::Error> {
        let mut file = File::open(file_path)?;
        let mut contents = String::new();
        file.read_to_string(&mut contents)?;
        let configs: SchemaConfigs = serde_yaml::from_str(&contents)?;
        Ok(configs)
    }
    
    // 从JSON文件加载配置
    pub fn load_from_json<P: AsRef<Path>>(file_path: P) -> Result<SchemaConfigs, anyhow::Error> {
        let mut file = File::open(file_path)?;
        let mut contents = String::new();
        file.read_to_string(&mut contents)?;
        let configs: SchemaConfigs = serde_json::from_str(&contents)?;
        Ok(configs)
    }
    
    // 将配置转换为表结构定义
    pub fn to_table_schema(config: &TableConfig) -> TableSchema {
        let fields = config.fields.iter().map(|field| {
            let mut constraints = field.constraints.clone();
            
            // 处理is_nullable属性
            if let Some(is_nullable) = field.is_nullable {
                if !is_nullable {
                    constraints.push("NOT NULL".to_string());
                }
            }
            
            TableField {
                name: field.name.clone(),
                sql_type: field.sql_type.clone(),
                constraints,
            }
        }).collect();
        
        TableSchema {
            name: config.name.clone(),
            fields,
            primary_key: config.primary_key.clone(),
        }
    }
}