from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
import os
import requests

app = FastAPI(title="Aion Game Data Web Service")

# 设置模板文件夹
TEMPLATES_DIR = os.path.join(os.path.dirname(__file__), "HTML/templates")
STATIC_DIR = os.path.join(os.path.dirname(__file__), "HTML/static")

# 创建模板和静态文件目录
os.makedirs(TEMPLATES_DIR, exist_ok=True)
os.makedirs(STATIC_DIR, exist_ok=True)

# 挂载静态文件
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

templates = Jinja2Templates(directory=TEMPLATES_DIR)

# 主页路由
@app.get("/", response_class=HTMLResponse)
async def read_root(request: Request):
    return templates.TemplateResponse("index.html", {"request": request, "title": "Aion Game Data"})

# 技能查询路由
@app.get("/skills", response_class=HTMLResponse)
async def read_skill(request: Request):
    return templates.TemplateResponse("skills.html", {"request": request, "title": "Skill Query"})

# 物品查询路由
@app.get("/items", response_class=HTMLResponse)
async def read_item(request: Request):
    return templates.TemplateResponse("items.html", {"request": request, "title": "Item Query"})

# NPC查询路由
@app.get("/npcs", response_class=HTMLResponse)
async def read_npc(request: Request):
    return templates.TemplateResponse("npcs.html", {"request": request, "title": "NPC Query"})

# 导入其他模块路由
from api.urls import router
app.include_router(router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)