use quick_xml::events::BytesStart;
use std::collections::HashMap;

use crate::utils::db_utils::{DataRecord, TableSchema, TableField};
use crate::utils::xml_parser::XmlTemplateParser;

// 技能模板解析器 - 专注于技能数据的解析
pub struct SkillTemplateParser;

impl XmlTemplateParser for SkillTemplateParser {
    fn get_table_name(&self) -> String {
        "skill".to_string()
    }
    
    fn get_table_schema(&self) -> TableSchema {
        TableSchema {
            name: "skill".to_string(),
            fields: vec![
                TableField {
                    name: "id".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec!["NOT NULL".to_string()],
                },
                TableField {
                    name: "name".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec!["NOT NULL".to_string()],
                },
                TableField {
                    name: "name_id".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec!["NOT NULL".to_string()],
                },
                TableField {
                    name: "level".to_string(),
                    sql_type: "INTEGER".to_string(),
                    constraints: vec!["NOT NULL".to_string()],
                },
                // 添加可能从XML中解析出的其他字段
                TableField {
                    name: "skilltype".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "skillsubtype".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "activation".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "hostile_type".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "stack".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "group".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "cancel_rate".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "cooldown".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "cooldownId".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "tslot".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "tslot_level".to_string(),
                    sql_type: "INTEGER".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "dispel_category".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "req_dispel_level".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "duration".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "skill_category".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "pvp_duration".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "chain_skill_prob".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "noremoveatdie".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "no_save_on_logout".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "ammospeed".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "ground".to_string(),
                    sql_type: "BOOLEAN".to_string(),
                    constraints: vec![],
                },
            ],
            primary_key: Some("id".to_string()),
        }
    }
    
    fn get_id_field(&self) -> String {
        "id".to_string()
    }
    
    fn parse_element(&self, element: &BytesStart, translations: &HashMap<String, String>) -> Option<DataRecord> {
        let mut record = HashMap::new();
        
        // 获取表结构中定义的所有字段名称
        let table_schema = self.get_table_schema();
        let mut allowed_fields = std::collections::HashSet::new();
        for field in &table_schema.fields {
            allowed_fields.insert(field.name.clone());
        }

        // 设置默认值
        record.insert("level".to_string(), "0".to_string());

        // 遍历所有属性
        for attr in element.attributes() {
            if let Ok(attr) = attr {
                let attr_name = attr.key.as_ref();
                if let Ok(attr_value) = attr.unescape_value() {
                    let value = attr_value.to_string();
                    
                    // 处理特殊属性和普通属性
                    match attr_name {
                        b"skill_id" => {
                            // 只同步表结构中定义的字段
                            if allowed_fields.contains("id") {
                                record.insert("id".to_string(), value);
                            }
                        },
                        b"nameId" => {
                            // nameId属性映射到name_id字段
                            if allowed_fields.contains("name_id") {
                                record.insert("name_id".to_string(), value.clone());
                            }
                            // 从翻译表中获取名称 - 只在找到翻译时替换
                            if allowed_fields.contains("name") {
                                if let Some(translated_name) = translations.get(&value) {
                                    record.insert("name".to_string(), translated_name.to_string());
                                }
                            }
                        },
                        b"lvl" => {
                            // 只同步表结构中定义的字段
                            if allowed_fields.contains("level") {
                                record.insert("level".to_string(), value);
                            }
                        },
                        // 其他属性只有在表结构中定义时才映射
                        _ => {
                            if let Ok(field_name) = std::str::from_utf8(attr_name) {
                                // 只同步表结构中定义的字段
                                if allowed_fields.contains(field_name) {
                                    record.insert(field_name.to_string(), value);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 确保id字段存在
        if record.contains_key("id") {
            Some(record)
        } else {
            None
        }
    }
}