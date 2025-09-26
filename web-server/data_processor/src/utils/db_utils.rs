use rusqlite::{Connection, Result};
use std::collections::HashMap;

// 表字段定义
#[derive(Debug, Clone)]
pub struct TableField {
    pub name: String,
    pub sql_type: String,
    pub constraints: Vec<String>,
}

// 表结构定义
#[derive(Debug, Clone)]
pub struct TableSchema {
    pub name: String,
    pub fields: Vec<TableField>,
    pub primary_key: Option<String>,
}

// 通用数据记录（用于存储从XML解析的数据）
pub type DataRecord = HashMap<String, String>;

// 确保表存在且结构正确
pub fn ensure_table_exists(conn: &mut Connection, schema: &TableSchema) -> Result<()> {
    // 检查表是否存在
    let table_exists: bool = conn.query_row(
        "SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type='table' AND name=?)",
        [&schema.name],
        |row| row.get(0)
    )?;

    if !table_exists {
        // 创建新表
        create_table(conn, schema)?;
    } else {
        // 检查表结构并更新
        update_table_structure(conn, schema)?;
    }

    Ok(())
}

// 创建新表
fn create_table(conn: &mut Connection, schema: &TableSchema) -> Result<()> {
    let mut field_defs = Vec::new();

    // 构建字段定义 - 对字段名添加引号以避免SQL保留字问题
    for field in &schema.fields {
        let mut def = format!("\"{}\"", field.name);
        def.push_str(" ");
        def.push_str(&field.sql_type);
        
        for constraint in &field.constraints {
            def.push_str(" ");
            def.push_str(constraint);
        }
        
        field_defs.push(def);
    }

    // 构建CREATE TABLE语句
    let mut create_sql = format!("CREATE TABLE {} (", schema.name);
    
    // 逐个添加字段定义，明确添加逗号分隔
    for (i, field_def) in field_defs.iter().enumerate() {
        if i > 0 {
            create_sql.push_str(", ");
        }
        create_sql.push_str(field_def);
    }
    
    // 添加主键约束（如果没有在fields中设置）
    if let Some(primary_key) = &schema.primary_key {
        // 检查fields中是否已经包含主键约束
        let has_primary_key_constraint = field_defs.iter().any(|def| {
            def.contains(&format!("PRIMARY KEY (\"{}\")", primary_key))
        });
        
        if !has_primary_key_constraint {
            if !field_defs.is_empty() {
                create_sql.push_str(", ");
            }
            create_sql.push_str(&format!("PRIMARY KEY (\"{}\")", primary_key));
        }
    }
    
    create_sql.push_str(")");
    
    // 添加日志输出以调试SQL语句
    println!("生成的SQL语句: {}", create_sql);

    conn.execute(&create_sql, [])?;
    Ok(())
}

// 更新表结构（添加缺失的字段）
fn update_table_structure(conn: &mut Connection, schema: &TableSchema) -> Result<()> {
    // 获取当前表结构
    // SQLite不支持在PRAGMA语句中使用参数占位符，所以需要使用字符串拼接
    let pragma_sql = format!("PRAGMA table_info({})", schema.name);
    let mut stmt = conn.prepare(&pragma_sql)?;
    let columns = stmt.query_map([], |row| {
        let name: String = row.get(1)?;
        Ok(name)
    })?;

    // 收集现有字段名
    let mut existing_columns = Vec::new();
    for column in columns {
        if let Ok(name) = column {
            existing_columns.push(name);
        }
    }

    // 添加缺失的字段 - 对字段名添加引号以避免SQL保留字问题
    for field in &schema.fields {
        if !existing_columns.contains(&field.name) {
            let mut alter_sql = format!("ALTER TABLE {} ADD COLUMN \"{}\"", schema.name, field.name);
            alter_sql.push_str(" ");
            alter_sql.push_str(&field.sql_type);
            
            // 为新添加的字段添加非NOT NULL约束可能会失败
            // 因此我们需要调整约束
            let mut modified_constraints = Vec::new();
            for constraint in &field.constraints {
                if constraint.to_uppercase() != "NOT NULL" {
                    modified_constraints.push(constraint.clone());
                }
            }
            
            for constraint in &modified_constraints {
                alter_sql.push_str(" ");
                alter_sql.push_str(constraint);
            }
            
            conn.execute(&alter_sql, [])?;
        }
    }

    Ok(())
}

// 插入或更新数据
pub fn upsert_data(
    conn: &mut Connection,
    table_name: &str,
    records: &[DataRecord],
    id_field: &str
) -> Result<()> {
    let tx = conn.transaction()?;

    for record in records {
        // 检查记录是否已存在
        let exists: bool = tx.query_row(
            &format!("SELECT EXISTS(SELECT 1 FROM {} WHERE \"{}\" = ?)", table_name, id_field),
            [record.get(id_field).unwrap_or(&"".to_string())],
            |row| row.get(0)
        )?;

        if exists {
            // 更新现有记录
            let mut fields = Vec::new();
            
            for key in record.keys() {
                if key != id_field {
                    fields.push(format!("\"{}\" = ?", key));
                }
            }
            
            if !fields.is_empty() {
                let update_sql = format!(
                    "UPDATE {} SET {} WHERE \"{}\" = ?",
                    table_name,
                    fields.join(", "),
                    id_field
                );
                
                // 准备参数数组
                let mut params: Vec<&dyn rusqlite::ToSql> = Vec::new();
                let empty_string = "".to_string();
                for key in record.keys() {
                    if key != id_field {
                        params.push(record.get(key).unwrap_or(&empty_string));
                    }
                }
                params.push(record.get(id_field).unwrap_or(&empty_string));
                
                tx.execute(&update_sql, params.as_slice())?;
            }
        } else {
            // 插入新记录
            // 直接构建SQL字符串，避免任何可能的字符串连接问题
            let mut insert_sql = format!("INSERT INTO {} (", table_name);
            
            let keys: Vec<&String> = record.keys().collect();
            for (i, key) in keys.iter().enumerate() {
                if i > 0 {
                    insert_sql.push_str(", ");
                }
                // 对字段名添加引号，避免SQL保留字问题
                insert_sql.push_str(&format!("\"{}\"", key));
            }
            
            insert_sql.push_str(") VALUES (");
            
            for (i, _) in keys.iter().enumerate() {
                if i > 0 {
                    insert_sql.push_str(", ");
                }
                insert_sql.push_str("?");
            }
            
            insert_sql.push_str(")");
            
            // 准备参数数组
            let mut params: Vec<&dyn rusqlite::ToSql> = Vec::new();
            for value in record.values() {
                params.push(value);
            }
            
            tx.execute(&insert_sql, params.as_slice())?;
        }
    }

    tx.commit()?;
    Ok(())
}