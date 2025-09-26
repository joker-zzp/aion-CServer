/**
 * 数据处理服务
 * 将 XML 文件同步到数据库
 * 
 * 使用方法: 命令行加参数 [--key 模板关键词] [--file <XML文件路径>] [--root <XML根元素>] [--l10n <翻译文件路径>] [--l10n-root <翻译文件根元素>]
 * 例如:
 *      ./data_processor --key skill --file /path/to/skills.xml --root skill_data --l10n /path/to/translations.xml --l10n-root strings
 * 说明:
 *      --key: 模板关键词 用于识别不同的模板类型, 例如 skill 或 item
 *      --file: XML文件路径, 指向要解析的XML文件
 *      --root: XML根元素, 用于指定XML文件中的根元素
 *      --l10n: 翻译文件路径, 可多次指定
 *      --l10n-root: 翻译文件中的根元素
 *      --db: SQLite数据库文件路径 (可选, 默认: ./data.db)
 *      --config: 配置文件路径 (可选, 默认: ./configs/{key}_config.yaml)
 */

use rusqlite::{Connection, Result};
use structopt::StructOpt;

// 导入库中的模块
extern crate data_processor;
use data_processor::utils::db_utils::{ensure_table_exists, upsert_data};
use data_processor::utils::xml_parser::{XmlParser, XmlParserFactory, XmlTemplateParser};
use data_processor::utils::l10n_utils::load_translations;
use data_processor::config::schema_config::SchemaConfigManager;
use data_processor::business_parsers::config_based_parser::ConfigParserFactory;

// 命令行参数结构体
#[derive(StructOpt, Debug)]
struct Opt {
    #[structopt(long)]
    key: String,
    
    #[structopt(long)]
    file: String,
    
    #[structopt(long)]
    root: String,
    
    #[structopt(long = "l10n", multiple = true)]
    l10n_files: Vec<String>,
    
    #[structopt(long = "l10n-root")]
    l10n_root: String,
    
    #[structopt(long, default_value = "./data.db")]
    db: String,
    
    #[structopt(long)]
    config: Option<String>,
}

// 主函数
fn main() {
    // 解析命令行参数
    let opt = Opt::from_args();
    
    // 初始化数据库
    let mut conn = init_database(&opt.db).expect("Failed to initialize database");
    
    // 处理数据
    process_data(&opt, &mut conn).expect("Failed to process data");
    
    println!("Data synchronization completed successfully!");
}

// 初始化数据库
fn init_database(db_path: &str) -> Result<Connection> {
    let conn = Connection::open(db_path)?;
    
    // 这里不再直接创建表，而是在process_data中根据解析器类型动态创建/更新表
    
    Ok(conn)
}

// 处理数据
fn process_data(opt: &Opt, conn: &mut Connection) -> Result<()> {
    // 加载翻译数据
    let translations = load_translations(&opt.l10n_files, &opt.l10n_root);
    
    // 使用解析器工厂创建对应的解析器
    let parser: Box<dyn XmlTemplateParser> = match XmlParserFactory::create_parser(&opt.key) {
        Some(p) => p,
        None => {
            // 尝试从配置文件加载配置
            match load_config_based_parser(&opt.key, &opt.config) {
                Some(p) => p,
                None => {
                    eprintln!("Unknown template key: {}", opt.key);
                    return Err(rusqlite::Error::ExecuteReturnedResults);
                }
            }
        },
    };
    
    // 获取表名和表结构
    let table_name = parser.get_table_name();
    let table_schema = parser.get_table_schema();
    
    // 确保表存在，如果不存在则创建，如果结构有变化则更新
    ensure_table_exists(conn, &table_schema)?;
    
    // 使用XmlParser工具类解析XML文件
    let template_element_name_str = format!("{}_template", opt.key);
    let template_element_name = template_element_name_str.as_bytes();
    let root_element_name = opt.root.as_bytes();
    
    // 解析XML文件获取数据记录
    let records = XmlParser::parse_xml_file(
        &opt.file,
        root_element_name,
        template_element_name,
        &*parser,
        &translations
    );
    
    // 批量处理数据
    let batch_size = 1000;
    let mut current_batch = Vec::new();
    let id_field = parser.get_id_field();
    
    for record in records {
        current_batch.push(record);
        
        // 如果批次满了，插入数据库
        if current_batch.len() >= batch_size {
            upsert_data(conn, &table_name, &current_batch, &id_field)?;
            current_batch.clear();
        }
    }
    
    // 处理剩余的批次
    if !current_batch.is_empty() {
        upsert_data(conn, &table_name, &current_batch, &id_field)?;
    }
    
    Ok(())
}

// 从配置文件加载基于配置的解析器
fn load_config_based_parser(key: &str, config_path: &Option<String>) -> Option<Box<dyn XmlTemplateParser>> {
    // 确定配置文件路径
    let path = match config_path {
        Some(p) => p.to_string(),
        None => format!("configs/{}_config.yaml", key),
    };
    
    println!("Loading configuration from: {}", path);
    
    // 加载配置
    match SchemaConfigManager::load_from_yaml(&path) {
        Ok(configs) => {
            let factory = ConfigParserFactory::new(configs);
            factory.create_parser(key)
        },
        Err(e) => {
            eprintln!("Failed to load configuration from {}: {:?}", path, e);
            None
        },
    }
}