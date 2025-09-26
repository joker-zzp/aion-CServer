use std::collections::HashMap;
use quick_xml::events::BytesStart;

use crate::utils::db_utils::{DataRecord, TableSchema, TableField};
use crate::utils::xml_parser::XmlTemplateParser;

// 物品模板解析器 - 专注于物品数据的解析
pub struct ItemTemplateParser;
impl XmlTemplateParser for ItemTemplateParser {
    fn get_table_name(&self) -> String {
        "item".to_string()
    }
    
    fn get_table_schema(&self) -> TableSchema {
        TableSchema {
            name: "item".to_string(),
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
                    constraints: vec![],
                },
                TableField {
                    name: "level".to_string(),
                    sql_type: "INTEGER".to_string(),
                    constraints: vec!["NOT NULL".to_string()],
                },
                TableField {
                    name: "item_type".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec!["NOT NULL".to_string()],
                },
                TableField {
                    name: "item_group".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "level_required".to_string(),
                    sql_type: "INTEGER".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "subtype".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "rarity".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "max_enchant".to_string(),
                    sql_type: "INTEGER".to_string(),
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
        record.insert("item_type".to_string(), "".to_string());
        
        // 遍历所有属性
        for attr in element.attributes() {
            if let Ok(attr) = attr {
                let attr_name = attr.key.as_ref();
                if let Ok(attr_value) = attr.unescape_value() {
                    let value = attr_value.to_string();
                    
                    // 处理特殊属性和普通属性
                    match attr_name {
                        b"id" => { record.insert("id".to_string(), value); },
                        b"desc" => {
                            // desc属性映射到name_id字段
                            if allowed_fields.contains("name_id") {
                                record.insert("name_id".to_string(), value.clone());
                            }
                            // 从翻译表中获取名称
                            if allowed_fields.contains("name") {
                                if let Some(translated_name) = translations.get(&value) {
                                    record.insert("name".to_string(), translated_name.to_string());
                                }
                            }
                        },
                        // 其他属性直接映射到同名字段
                        _ => {
                            if let Ok(field_name) = std::str::from_utf8(attr_name) {
                                // 只同步在表结构中定义的字段才映射
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