from fastapi import Request, APIRouter
import requests
import os
import json
import subprocess

router = APIRouter(
    prefix="/api",
    tags=["api"],
    responses={404: {"description": "Not found"}},
)

RUST_SERVER = "http://localhost:8080"

def api_request(res, api, params = {}):
    try:
        url = f"{RUST_SERVER}{api}"
        
        response = requests.get(url, params = params)
        if response.status_code == 200:
            res_data = response.json()
            if res_data.get('data'):
                res["data"] = res_data.get('data', [])
            if res_data.get('total'):
                res["total"] = res_data.get('total', 0)
        else:
            return {
                "code": response.status_code,
                "msg": response.text
            }
    except Exception as e:
        res = {
            "code": -1,
            "msg": "服务错误"
        }
    else:
        res["code"] = 0
        res["msg"] = "查询成功"
    return res

# 读取配置文件
import configparser

config = configparser.ConfigParser()
config.read('config/service.conf')

def get_config_value(title, key):
    # 从配置文件中获取值并移除引号
    # 去除前后的引号
    return str(config.get(title, key)).strip('"').replace('\\', '/')

# 同步数据任务队列
task = []

# 启动命令线程同步数据
def sync_data(**kwargs):
    bin_path = os.path.join(os.getcwd(), get_config_value('bin', 'dir'), 'data_processor')
    db_file = os.path.join(os.getcwd(), get_config_value('db', 'db_file'))
    # 同步技能数据
    # 创建一个子进程来执行同步命令
    

    command_args = [bin_path, '--db', db_file]
    for k, v in kwargs.items():
        # 将下划线替换成-
        k = k.replace('_', '-')
        if isinstance(v, list):
            for it in v:
                command_args.extend([f"--{k}", str(it)])
        else:
            command_args.extend([f"--{k}", str(v)])

    print(f"启动异步任务, 命令: {' '.join(command_args)}")
    task.append(subprocess.Popen(command_args, cwd=os.getcwd()))
    return len(task)

# 查询数据
import sqlite3
import os

def query_table(table_name, fields = [], page=1, size=10, search=None):
    # 连接数据库 - 根据Rust代码，数据库文件在data_processor目录下
    db_path = os.path.join(os.getcwd(), get_config_value('db', 'db_file'))
    
    try:
        # 连接SQLite数据库
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 构建字段列表
        fields_str = '*' if not fields else ', '.join(fields)
        
        # 构建查询语句
        query = f"SELECT {fields_str} FROM {table_name}"
        where_clause = ""
        params = []
        
        # 添加搜索条件
        if search:
            search_conditions = []
            for field in ['name', 'id', 'name_id']:
                if field in (fields if fields else ['id', 'name', 'name_id', 'level']):
                    search_conditions.append(f"{field} LIKE ?")
            
            if search_conditions:
                where_clause = f" WHERE {' OR '.join(search_conditions)}"
                params.extend([f"%{search}%"] * len(search_conditions))
        
        # 先查询总数
        count_query = f"SELECT COUNT(*) FROM {table_name}{where_clause}"
        cursor.execute(count_query, params)
        total = cursor.fetchone()[0]
        
        # 构建完整的查询语句
        query += where_clause
        
        # 添加分页
        offset = (page - 1) * size
        query += " LIMIT ? OFFSET ?"
        params.extend([size, offset])
        
        # 执行查询
        cursor.execute(query, params)
        
        # 获取列名
        columns = [desc[0] for desc in cursor.description]
        
        # 获取查询结果
        results = []
        for row in cursor.fetchall():
            # 将每一行转换为字典
            results.append(dict(zip(columns, row)))
        
        # 关闭连接
        cursor.close()
        conn.close()
        
        return {
            'code': 0,
            'msg': '查询成功',
            'data': results,
            'total': total,
            'page': page,
            'size': size
        }
    except sqlite3.Error as e:
        return {
            'code': -1,
            'msg': f'数据库错误: {str(e)}',
            'data': [],
            'total': 0,
            'page': page,
            'size': size
        }
    except Exception as e:
        return {
            'code': -2,
            'msg': f'查询错误: {str(e)}',
            'data': [],
            'total': 0,
            'page': page,
            'size': size
        }

rask_kwargs = {
    "skill": {
        "key": "skill",
        "root": "skill_data",
        "file": "static_data/skills/skill_templates.xml",
        "l10n_root": "strings",
        "l10n": ["l10n/CHS/Data/strings/client_strings_skill.xml"],
        "config": "config/skill_config.yaml",
    },
    "item": {
        "key": "item",
        "root": "item_templates",
        "file": "static_data/items/item_templates.xml",
        "l10n_root": "strings",
        "l10n": ["l10n/CHS/Data/strings/client_strings_item.xml", "l10n/CHS/Data/strings/client_strings_item2.xml", "l10n/CHS/Data/strings/client_strings_item3.xml"],
        "config": "config/item_config.yaml",
    },
    "npc": {
        "key": "npc",
        "root": "npc_templates",
        "file": "static_data/npcs/npc_templates.xml",
        "l10n_root": "strings",
        "l10n": ["l10n/CHS/Data/strings/client_strings_npc.xml"],
        "config": 'config/npc_config.yaml',
    },
}

# 同步数据
@router.get("/sync")
async def api_sync_data(key: str = None):
    res = {}
    # 从 任务字典中 获取任务参数并执行如果不存在就是未知错误

    if key not in rask_kwargs:
        res.update({
            "code": -1,
            "msg": f"未知的同步任务: {key}",
        })
        return res
    kwargs = rask_kwargs[key]
    t_count = sync_data(**kwargs)
    if t_count:
        res.update({
            "code": 0,
            "msg": f"同步任务已启动",
        })
    else:
        res.update({
            "code": -1,
            "msg": f"同步任务启动失败",
        })
    return res

# 查看当前任务列表状态
@router.get("/sync/task")
async def sync_task():
    res = {
        "code": 0,
        "msg": "查询成功",
        "data": [],
        "total": 0,
    }
    res_data = [{
        "id": i.pid,
        "status": i.poll(),
        "args": i.args
    } for i in task]
    res.update({
        "data": res_data,
        "total": len(task),
    })
    return res

# 清理已经完成的任务
@router.get("/sync/task/clean")
async def clean_task():
    global task
    task = [i for i in task if i.poll() is None]
    return {
        "code": 0,
        "msg": f"清理完成, 剩余任务数量: {len(task)}",
    }

@router.get("/npcs/query")
async def read_npc(q: str = '', page: int = 1, size: int = 10):
    res = {}
    page = max(1, page)
    size = min(100, max(1, size))  # 限制每页最多100条数据
    # 从数据库查询NPC数据
    result = query_table('npc', fields=['id', 'name_id', 'name', 'title', 'level'], page=page, size=size, search=q)
    res.update({
        "code": 0,
        "msg": "查询成功",
        "data": result['data'],
        "total": result['total'],
        "page": page,
        "size": size,
    })
    return res

@router.get("/skills/search")
async def read_skill(q: str = '', page: int = 1, size: int = 10):
    # 验证分页参数
    page = max(1, page)
    size = min(100, max(1, size))  # 限制每页最多100条数据
    
    # 从数据库搜索技能数据
    result = query_table('skill', fields=['id', 'name', 'name_id', 'level', 'skilltype', 'skillsubtype'], page=page, size=size, search=q)
    
    return result

@router.get("/items/search")
async def read_item(q: str = '', page: int = 1, size: int = 10):
    # 验证分页参数
    page = max(1, page)
    size = min(100, max(1, size))  # 限制每页最多100条数据
    
    # 从数据库搜索物品数据
    result = query_table('item', fields=['id', 'name', 'name_id', 'level', 'item_group'], page=page, size=size, search=q)
    
    return result