![Aion 4.8 Banner](https://github.com/beyond-aion/aion-server/assets/1169307/494205be-399a-4e2e-8435-1f0774d92262)
<div align="center">

  ![](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fgithub.com%2Fbeyond-aion%2Faion-server%2Fraw%2F4.8%2Fpom.xml&query=%2F%2A%5Blocal-name%28%29%3D%22project%22%5D%2F%2A%5Blocal-name%28%29%3D%22properties%22%5D%2F%2A%5Blocal-name%28%29%3D%22maven.compiler.release%22%5D%2Ftext%28%29&label=Java%20version)
  [![](https://img.shields.io/github/contributors-anon/beyond-aion/aion-server)](https://htmlpreview.github.io/?https://gist.github.com/neon-dev/ce9729bcacaac31f78771b8521512d0a/raw/contributors.html&repo=beyond-aion/aion-server&title=Beyond%20Aion%20Server%20Contributors)
  ![](https://img.shields.io/github/repo-size/beyond-aion/aion-server)

</div>

# Aion 4.8 Server Emulator

此项目为中文版本. 从开源 Aion-server 4.8 分支 fork 而来.

原项目文档([English](https://github.com/beyond-aion/aion-server/blob/4.8/README.md))

本项目为 Aion 4.8 服务器模拟器的中文版本, 目前修复了一些中文相关问题, 优化了部分代码

配置了docker-compose.yml, 可以直接使用docker-compose up -d 启动 所有游戏服务

在启动前, 请先配置环境变量, 在项目中 创建 `.env` 文件, 并配置好环境变量

```env
AION_HOME=
AION_LOG_PATH=
AION_DB_PATH=
AION_SERVER_IP=
AION_DB_NAME=
AION_DB_USER=
AION_DB_PASSWORD=
AION_GAME_PASSWORD=
```

`docker-compose -f ./aion-compose.yml up -d` 启动创建服务

启动完成后, 可以使用 `docker-compose -f ./aion-compose.yml ps` 查看服务状态  `docker logs -f aion-gs` 查看游戏服务日志
