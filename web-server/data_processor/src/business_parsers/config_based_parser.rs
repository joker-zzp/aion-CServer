use std::collections::HashMap;
use std::sync::Arc;
use quick_xml::events::BytesStart;

use crate::utils::db_utils::{DataRecord, TableSchema};
use crate::utils::xml_parser::XmlTemplateParser;
use crate::config::schema_config::{SchemaConfigManager, TableConfig, SchemaConfigs, SpecialFieldConfig};

// 基于配置的通用解析器
pub struct ConfigBasedParser {
    config: Arc<TableConfig>,
}

impl ConfigBasedParser {
    pub fn new(config: TableConfig) -> Self {
        Self {
            config: Arc::new(config),
        }
    }
    
    // 处理特殊字段
    fn handle_special_field(
        &self, 
        attr_name: &str, 
        attr_value: &str, 
        config: &SpecialFieldConfig, 
        record: &mut DataRecord, 
        translations: &HashMap<String, String>
    ) {
        // 确定目标字段名
        let target_field = config.target_field.as_deref().unwrap_or(attr_name);
        
        // 记录原始值
        record.insert(target_field.to_string(), attr_value.to_string());
        
        // 处理翻译
        if let Some(true) = config.translate {
            // 翻译字段名通常是原始字段名加上"_translated"后缀
            let translated_field = format!("{}_translated", target_field);
            let translated_value = translations.get(attr_value).cloned().unwrap_or_else(|| attr_value.to_string());
            record.insert(translated_field, translated_value.clone());
        }
    }
}

impl XmlTemplateParser for ConfigBasedParser {
    fn get_table_name(&self) -> String {
        self.config.name.clone()
    }
    
    fn get_table_schema(&self) -> TableSchema {
        SchemaConfigManager::to_table_schema(&self.config)
    }
    
    fn get_id_field(&self) -> String {
        self.config.id_field.clone()
    }
    
    fn parse_element(&self, element: &BytesStart, translations: &HashMap<String, String>) -> Option<DataRecord> {
        let mut record = HashMap::new();
        let id_field = &self.config.id_field;
        
        // 首先设置默认值
        for field in &self.config.fields {
            if let Some(default_value) = &field.default_value {
                record.insert(field.name.clone(), default_value.clone());
            }
        }
        
        // 遍历所有属性
        for attr in element.attributes() {
            if let Ok(attr) = attr {
                let attr_name = attr.key.as_ref();
                if let Ok(attr_value) = attr.unescape_value() {
                    let value = attr_value.to_string();
                    
                    // 尝试将属性名从UTF-8字节转换为字符串
                    if let Ok(field_name) = std::str::from_utf8(attr_name) {
                        // 检查是否是特殊处理的字段
                        if let Some(special_fields) = &self.config.special_fields {
                            if let Some(special_config) = special_fields.get(field_name) {
                                // 处理特殊字段
                                self.handle_special_field(
                                    field_name, 
                                    &value, 
                                    special_config, 
                                    &mut record, 
                                    translations
                                );
                                continue;
                            }
                        }
                        
                        // 普通字段直接映射
                        record.insert(field_name.to_string(), value);
                    }
                }
            }
        }
        
        // 确保主键字段存在
        if record.contains_key(id_field) {
            Some(record)
        } else {
            None
        }
    }
}

// 配置解析器工厂扩展 - 支持从配置创建解析器
pub struct ConfigParserFactory {
    configs: Arc<SchemaConfigs>,
}

impl ConfigParserFactory {
    pub fn new(configs: SchemaConfigs) -> Self {
        Self {
            configs: Arc::new(configs),
        }
    }
    
    pub fn create_parser(&self, template_type: &str) -> Option<Box<dyn XmlTemplateParser>> {
        if let Some(config) = self.configs.get(template_type) {
            Some(Box::new(ConfigBasedParser::new(config.clone())))
        } else {
            None
        }
    }
}