use std::path::Path;
use std::collections::HashMap;
use quick_xml::{Reader, events::{Event, BytesStart}};
use std::fs::File;
use std::io::{BufReader, Read};

// 从XML属性中提取值
#[allow(dead_code)]
pub fn extract_attribute<'a>(element: &BytesStart<'a>, name: &[u8]) -> Option<String> {
    for attr in element.attributes().flatten() {
        if attr.key.as_ref() == name {
            let result = String::from_utf8_lossy(&attr.value).to_string();
            return Some(result);
        }
    }
    None
}

// XML模板解析器特性 - 定义了通用的XML数据解析接口
pub trait XmlTemplateParser {
    // 获取表名
    fn get_table_name(&self) -> String;
    
    // 获取表结构定义
    fn get_table_schema(&self) -> crate::utils::db_utils::TableSchema;
    
    // 获取主键字段名
    fn get_id_field(&self) -> String;
    
    // 解析XML元素为数据记录
    fn parse_element(&self, element: &BytesStart, translations: &HashMap<String, String>) -> Option<crate::utils::db_utils::DataRecord>;
}

// 通用XML解析工具 - 处理底层XML读取和解析逻辑
pub struct XmlParser;

impl XmlParser {
    // 加载翻译数据（支持嵌套结构的string元素和UTF-16LE编码）
    pub fn load_translations(files: &[String], root_element: &str) -> HashMap<String, String> {
        let mut translations = HashMap::new();
        
        for file_path in files {
            // 尝试使用UTF-16LE编码打开文件
            if let Ok(file) = File::open(file_path) {
                // 读取文件并转换为UTF-8
                let mut contents = String::new();
                {
                    let mut reader = BufReader::new(file);
                    // 读取文件内容
                    let mut data = Vec::new();
                    reader.read_to_end(&mut data).ok();
                    
                    // 跳过BOM（如果存在）
                    let data_slice = if data.len() >= 2 && data[0] == 0xFF && data[1] == 0xFE {
                        &data[2..]
                    } else {
                        &data[..]
                    };
                    
                    // 转换UTF-16LE到UTF-8
                    if data_slice.len() % 2 == 0 { // 确保字节数是偶数
                        let utf16_chars: Vec<u16> = data_slice
                            .chunks_exact(2)
                            .map(|chunk| u16::from_le_bytes([chunk[0], chunk[1]]))
                            .collect();
                        
                        if let Ok(s) = String::from_utf16(&utf16_chars) {
                            contents = s;
                        }
                    }
                }
                
                // 使用转换后的UTF-8内容进行XML解析
                let mut xml_reader = Reader::from_str(&contents);
                xml_reader.trim_text(true);
                
                // 设置编码处理
                xml_reader.check_end_names(false);
                
                let mut buf = Vec::new();
                let mut inside_root = false;
                let mut inside_string = false;
                let mut current_id = String::new();
                let mut current_name = String::new();
                let mut current_body = String::new();
                let mut current_element = String::new();
                
                loop {
                    match xml_reader.read_event_into(&mut buf) {
                        Ok(Event::Start(ref e)) => {
                            let name_bytes = e.name();
                            let name = String::from_utf8_lossy(name_bytes.as_ref()).to_string();
                            
                            if name == root_element {
                                inside_root = true;
                            } else if inside_root && name == "string" {
                                inside_string = true;
                                current_id.clear();
                                current_name.clear();
                                current_body.clear();
                            } else if inside_string {
                                current_element = name;
                            }
                        },
                        Ok(Event::Text(ref e)) => {
                            if inside_string && !current_element.is_empty() {
                                if let Ok(unescaped) = e.unescape() {
                                    let text = unescaped.to_string();
                                    match current_element.as_str() {
                                        "id" => current_id = text,
                                        "name" => current_name = text,
                                        "body" => current_body = text,
                                        _ => ()
                                    }
                                }
                            }
                        },
                        Ok(Event::End(ref e)) => {
                            let name_bytes = e.name();
                            let name = String::from_utf8_lossy(name_bytes.as_ref()).to_string();
                            
                            if name == root_element {
                                inside_root = false;
                            } else if inside_string && name == "string" {
                                // 当string元素结束时，保存翻译数据
                                if !current_id.is_empty() && !current_body.is_empty() {
                                    translations.insert(current_id.clone(), current_body.clone());
                                }
                                inside_string = false;
                                current_element.clear();
                            } else if inside_string && name == current_element {
                                current_element.clear();
                            }
                        },
                        Ok(Event::Eof) => break,
                        Err(e) => {
                            eprintln!("Error parsing translation file {} at position {}: {}", 
                                     file_path, xml_reader.buffer_position(), e);
                            break;
                        },
                        _ => ()
                    }
                    buf.clear();
                }
            } else {
                eprintln!("Failed to open translation file: {}", file_path);
            }
        }
        
        translations
    }
    
    // 处理XML文件，将模板元素解析为数据记录
    pub fn parse_xml_file<P: XmlTemplateParser + ?Sized>(
        file_path: &str,
        root_element: &[u8],
        template_element: &[u8],
        parser: &P,
        translations: &HashMap<String, String>
    ) -> Vec<crate::utils::db_utils::DataRecord> {
        let mut records = Vec::new();
        
        // 检查文件是否存在
        if !Path::new(file_path).exists() {
            eprintln!("XML file not found: {}", file_path);
            return records;
        }
        
        match File::open(file_path) {
            Ok(file) => {
                let buf_reader = BufReader::new(file);
                let mut reader = Reader::from_reader(buf_reader);
                reader.trim_text(true);
                
                let mut buf = Vec::new();
                let mut inside_root = false;
                
                loop {
                    match reader.read_event_into(&mut buf) {
                        Ok(Event::Start(ref e)) => {
                            // 将name保存到临时变量，延长生命周期
                            let name_bytes = e.name();
                            let element_name = name_bytes.as_ref();
                            
                            // 检查是否到达根元素
                            if element_name == root_element {
                                inside_root = true;
                            }
                            // 如果在根元素内部且是目标模板元素
                            else if inside_root && element_name == template_element {
                                if let Some(record) = parser.parse_element(e, translations) {
                                    records.push(record);
                                }
                            }
                        },
                        Ok(Event::End(ref e)) => {
                            // 检查是否离开根元素
                            if e.name().as_ref() == root_element {
                                inside_root = false;
                            }
                        },
                        Ok(Event::Eof) => break,
                        Err(e) => {
                            eprintln!("Error at position {}: {:?}", reader.buffer_position(), e);
                            break;
                        },
                        _ => (),
                    }
                    
                    // 清空缓冲区
                    buf.clear();
                }
            },
            Err(e) => {
                eprintln!("Failed to open XML file: {}", e);
            }
        }
        
        records
    }
}


// 导入业务解析器
use crate::business_parsers::{SkillTemplateParser, ItemTemplateParser, NpcTemplateParser};

// 解析器工厂 - 用于创建不同类型的解析器实例
pub struct XmlParserFactory;

impl XmlParserFactory {
    // 根据模板类型创建对应的解析器
    pub fn create_parser(template_type: &str) -> Option<Box<dyn XmlTemplateParser>> {
        match template_type {
            "skill" => Some(Box::new(SkillTemplateParser)),
            "item" => Some(Box::new(ItemTemplateParser)),
            "npc" => Some(Box::new(NpcTemplateParser)),
            _ => None
        }
    }
}

// 保留旧的load_translations函数以保持向后兼容
#[allow(dead_code)]
pub fn load_translations(files: &[String], root_element: &str) -> HashMap<String, String> {
    XmlParser::load_translations(files, root_element)
}