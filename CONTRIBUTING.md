# 贡献指南 · CONTRIBUTING

感谢关注尚枢 AI（ShangShu AI）！欢迎以任何形式参与贡献：提交 Issue、完善文档、修复 Bug、实现新特性。

## 环境准备

| 依赖 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 21+ | 后端编译与运行 |
| Maven | 3.9+ | 仓库自带 `mvnw` 包装器，可直接使用 |
| Node.js | 22+ | Web 管理台构建 |
| Docker & Docker Compose | 最新稳定版 | 启动 MySQL / Redis Stack / Elasticsearch / RabbitMQ |

## 本地开发

```bash
# 1. 启动中间件（MySQL / Redis / ES / RabbitMQ）
docker compose up -d

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，至少修改 AI_AUTH_JWT_SECRET 和 JASYPT_ENCRYPTOR_PASSWORD

# 3. 启动后端（dev profile）
cd api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 启动 Web 管理台
cd ai-agent-web
npm install
npm run dev
```

后端启动后可访问 `http://localhost:8080/swagger-ui.html` 查看 API 文档，`/actuator/health` 做健康检查。

## 代码规范

后端采用两层质量控制，二者互补：

- **Spotless（强制）**：负责代码格式化，CI 中执行 `mvn spotless:check` 校验。提交前请运行：

  ```bash
  cd api
  ./mvnw spotless:apply
  ```

- **Checkstyle（规则参考）**：规则定义在 [`api/checkstyle.xml`](api/checkstyle.xml)，依据《企业 Java 后端开发规范》制定，包括：
  - 命名规范（类 / 方法 / 常量 / 包名）
  - Import 规范（禁用 `*` 导入、顺序分组、去冗余）
  - 代码长度（文件 ≤ 800 行、单行 ≤ 160 字符）
  - 基础质量检查（空代码块、必须加大括号、equals/hashCode 成对出现等）

前端遵循 `ai-agent-web/AGENTS.md` 中的约定（TypeScript 严格模式、组件与 API 分层），后端遵循 `api/AGENTS.md`。

## 提交信息约定

采用 Conventional Commits 风格，描述用中文：

```
feat: 新增课程问答与微信小程序登录
fix(knowledge): 按vector_id批量更新命中计数
refactor: 清理废弃代码并优化客服对话与检索路由
chore: init project structure (ShangShu AI)
```

- 类型：`feat` / `fix` / `refactor` / `docs` / `test` / `chore`
- 可选 scope：模块名（如 `knowledge`、`workflow`）
- 一次提交只做一件事

## PR 流程

1. **Fork** 本仓库到个人账号（核心贡献者可直接建分支）
2. **建分支**：`git checkout -b feat/your-feature`（命名 `feat/`、`fix/`、`docs/` 前缀）
3. **开发并自测**：后端 `./mvnw spotless:apply && ./mvnw test`，前端 `npm run build`
4. **提交 PR**：目标分支 `main`，描述中说明改动动机、影响范围与自测结果
5. **CI 通过**：GitHub Actions 会自动执行后端 `spotless:check + test` 与前端 `npm run build`
6. **Review 合并**：维护者 review 后 squash merge

## 提交 Issue

- **Bug 报告**：使用 [Bug 报告模板](.github/ISSUE_TEMPLATE/bug_report.yml)，附复现步骤、期望/实际行为、环境信息与日志
- **特性建议**：使用 [特性建议模板](.github/ISSUE_TEMPLATE/feature_request.yml)，说明使用场景与期望方案
- **安全问题**：请勿公开提交，按 [SECURITY.md](SECURITY.md) 指引私密报告

新人贡献者可以从 [Good First Issues](docs/good-first-issues.md) 入手，每个任务都标注了涉及文件、验收标准与难度。

## 行为准则

保持友善与尊重，对事不对人；讨论聚焦技术本身。维护者保留对不符合社区规范的言论与内容的处理权。

## 许可证

提交即表示你同意代码以 [Apache License 2.0](LICENSE) 协议开源。
