use std::collections::HashMap;
use std::fs::File;
use std::io::{BufReader, Read};
use quick_xml::Reader;
use quick_xml::events::Event;

// 加载翻译数据
// 从多个XML文件中读取翻译字符串
// 参数:
// - files: 翻译文件路径列表
// - root_element: 翻译文件的根元素名称
// 返回: 翻译映射，键为ID，值为翻译文本
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