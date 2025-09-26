use quick_xml::events::BytesStart;
use std::collections::HashMap;

use crate::utils::db_utils::{DataRecord, TableSchema, TableField};
use crate::utils::xml_parser::XmlTemplateParser;

// 解析NPC模板
pub struct NpcTemplateParser;

impl XmlTemplateParser for NpcTemplateParser {

    fn get_table_name(&self) -> String {
        "npc".to_string()
    }

    fn get_id_field(&self) -> String {
        "id".to_string()
    }

    fn get_table_schema(&self) -> TableSchema {
        TableSchema {
            name: self.get_table_name().to_string(),
            fields: vec![
                TableField {
                    name: "id".to_string(),
                    sql_type: "INTEGER".to_string(),
                    constraints: vec!["NOT NULL".to_string()],
                },
                TableField {
                    name: "name_id".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "name".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "title".to_string(),
                    sql_type: "TEXT".to_string(),
                    constraints: vec![],
                },
                TableField {
                    name: "level".to_string(),
                    sql_type: "INTEGER".to_string(),
                    constraints: vec![],
                },
            ],
            primary_key: Some("id".to_string()),
        }
    }

    fn parse_element(&self, element: &BytesStart, translations: &HashMap<String, String>) -> Option<DataRecord> {
        let mut record = HashMap::new();
        // 获取表结构中定义的所有字段名称
        let table_schema = self.get_table_schema();
        let mut allowed_fields = std::collections::HashSet::new();
        for field in &table_schema.fields {
            allowed_fields.insert(field.name.clone());
        }

        for attr in element.attributes() {
            if let Ok(attr) = attr {
                let attr_name = attr.key.as_ref();
                if let Ok(attr_value) = attr.unescape_value() {
                    let value = attr_value.to_string();

                    match attr_name {
                        b"npc_id" => {
                            if allowed_fields.contains("id") {
                                record.insert("id".to_string(), value);
                            }
                        },
                        b"name_id" => {
                            if allowed_fields.contains("name_id") {
                                record.insert("name_id".to_string(), value.clone());
                            }
                            if allowed_fields.contains("name") {
                                if let Some(translated_name) = translations.get(&value) {
                                    record.insert("name".to_string(), translated_name.to_string());
                                }
                            }
                        },
                        b"title_id" => {
                            if allowed_fields.contains("title") {
                                if let Some(translated_title) = translations.get(&value) {
                                    record.insert("title".to_string(), translated_title.to_string());
                                }
                            }
                        },
                        // 其他属性在表结构中定义是才映射
                        _ => {
                            if let Ok(field_name) = std::str::from_utf8(attr_name) {
                                // 同步表结构中定义的字段
                                if allowed_fields.contains(field_name) {
                                    record.insert(field_name.to_string(), value);
                                }
                            }
                        }
                    }
                }
            }
        }
        // 确保id存在
        if record.contains_key("id") {
            Some(record)
        } else {
            None
        }
    }
}