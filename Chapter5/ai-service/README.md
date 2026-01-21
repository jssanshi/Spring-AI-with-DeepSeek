### 启动依赖服务
docker compose up -d

spring.ai.deepseek.api-key=<值修改为自己申请的key>

### 停止并删除容器
docker compose down

### non-streaming-ai-service
包含记忆、Advisors 功能

### streaming-ai-service
流式输出，包含完整前后端
* 会话列表管理：新建、保存、归档
* 对话记录管理：停止、重新生成、新建会话
* 对话记忆管理：存储、展示对话记忆
* 记忆库支持：内存、mysql、图neo4j、向量库