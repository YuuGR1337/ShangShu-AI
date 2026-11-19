# 企业 Java 后端通用开发规范

> 提取自企业级 OA 项目开发规范，剥离项目专有框架（Flowable 工作流、多租户 starter 等私有能力），保留通用 Spring Boot / MyBatis-Plus 企业级编码、架构、安全、数据库、接口、异常、工程规范。可直接用于个人项目、AI Agent 后端练习、简历项目、团队通用约束。

---

## 一、架构与分层规范

### 1.1 调用链路（强制单向依赖）

**调用链：Controller → Service → Mapper(DAL) → DB**

1. Controller 只做：接收参数 → 参数校验 → 调用 Service → 对象转换 → 返回结果。**禁止写业务逻辑、禁止直接调用 Mapper**。
2. Service 层：业务校验、业务编排、事务控制、对象转换；跨模块调用**只允许调用对方对外 API 接口 / DTO，禁止直接依赖对方 Service、Mapper、DO 实体**。
3. Mapper 层：仅负责数据库 CRUD，不写业务逻辑。
4. 禁止反向依赖：DAO / Mapper 不能引用 Service；禁止循环依赖，必须循环依赖时使用 `@Lazy` 并尽量重构消除。

### 1.2 通用业务模块分包模板

```
com.xxx.module.xxx
├── controller
│   └── admin              # 管理后台接口
│       └── vo             # ReqVO、RespVO
├── convert                # 对象转换 MapStruct / BeanUtils
├── dal
│   ├── dataobject         # DO 数据库实体
│   └── mysql              # Mapper 接口
├── enums                  # 业务枚举、错误码常量
├── service
│   ├── XxxService         # Service 接口
│   └── impl               # ServiceImpl 实现类
├── mq                     # 消息 message / producer / consumer（可选）
├── job                    # 定时任务（可选）
├── framework              # 模块自定义配置（可选）
└── util                   # 模块内部工具类（可选）
```

### 1.3 对象模型定义规范

| 对象 | 后缀 | 用途 |
|---|---|---|
| DO | XxxDO | 数据库实体，和表一一对应 |
| 新增 / 修改入参 | XxxSaveReqVO | Controller 接收保存、更新请求 |
| 分页入参 | XxxPageReqVO | 分页查询入参，继承通用 PageParam |
| 返回视图对象 | XxxRespVO | 返回前端响应对象 |
| 跨模块传输 | XxxReqDTO / XxxRespDTO | 模块间调用传输对象，**禁止复用 VO / DO** |

> 重要：DO、VO、DTO 严格分离；禁止直接把 DO 实体返回前端，敏感字段不能对外输出。

---

## 二、命名规范

### 2.1 基础命名

1. **包名**：全小写单数，禁止下划线、大写。
2. **类、接口、枚举**：大驼峰；工具类以 `Utils` 复数结尾；抽象类用 `Base` / `Abstract` 前缀；异常类以 `Exception` 结尾。
3. **方法 / 变量**：小驼峰；方法遵循「动词 + 名词」；获取统一用 `getXxx`，不要 `query / find`；判断方法以 `is / has / can` 开头返回 boolean。
4. **常量**：全大写，下划线分隔；禁止魔法数字、魔法字符串，全部抽常量或枚举。
5. **集合变量**：复数或后缀 `List / Map / Set`，例如 `userList`、`userMap`。
6. **禁止**：拼音命名、中英混合、无意义缩写；布尔字段**禁止 is 前缀**，避免序列化解析异常。

### 2.2 层方法统一动词命名

| 动作 | 方法名 |
|---|---|
| 新增 | createXxx |
| 修改 | updateXxx |
| 删除 | deleteXxx / deleteXxxList |
| 查询单条 | getXxx |
| 查询列表 | getXxxList |
| 分页查询 | getXxxPage |
| 业务校验 | validateXxxExists / validateXxxUnique |

### 2.3 REST 接口 URL 规范

1. URL 全小写，连字符分隔，名词形式。
2. HTTP 语义严格匹配：
   - POST → create 新增
   - PUT → update 修改
   - DELETE → delete 删除
   - GET → get / page / list 查询
3. 权限标识格式：`模块:功能:操作`，例：`chat:session:create`。

---

## 三、Controller 层编码规范

1. 类注解：`@RestController`、`@RequestMapping("/模块/功能")`、`@Validated`；接口文档注解 `@Tag`。
2. 每个接口添加 `@Operation(summary = "接口说明")`；入参使用 `@Valid` 做校验。
3. 权限注解：`@PreAuthorize` 做权限控制；无需登录接口统一白名单配置，不随意加 `@PermitAll`。
4. **所有返回必须统一包装通用结果对象 `CommonResult<T>`**，禁止直接返回裸对象。
5. Controller 内部不捕获业务异常，交给**全局异常处理器 `@ControllerAdvice`** 统一处理。

---

## 四、Service 层编码规范

1. Service 接口 + ServiceImpl 实现分离；实现类标注 `@Service`。
2. 写操作前置校验：私有校验方法 `validateXxxExists()`，校验失败直接抛出业务异常。
3. 多表写操作，必须添加声明式事务：`@Transactional(rollbackFor = Exception.class)`；**查询方法不加事务**。
4. 对象转换优先使用工具：`BeanUtils.toBean(source, Target.class)`，避免手写大量 setter。
5. **业务只抛出业务异常 BizException，禁止直接抛出 RuntimeException**；错误码使用常量，禁止硬编码字符串错误信息。
6. 注入：推荐 `@Resource`，不使用 `@Autowired`。
7. 禁止空 catch 吞异常；捕获异常必须打完整 error 日志，再向上抛出业务异常。

---

## 五、DAL / MyBatis-Plus 数据库层规范

### 5.1 DO 实体

1. DO 继承基础 `BaseDO`，自动填充公共字段：`id、creator、create_time、updater、update_time、deleted` 逻辑删除。
2. 数据库表：`模块_业务名` 蛇形小写；每张业务表必须包含：`id、creator、create_time、updater、update_time、deleted`。
3. 字段：蛇形命名；时间统一使用 `LocalDateTime`；状态使用 tinyint，配合枚举；长文本使用 text；金额用 decimal。
4. 索引命名：普通索引 `idx_xxx`；唯一索引 `uk_xxx`；表和字段必须写 COMMENT 注释。
5. 禁止物理删除，全部使用**逻辑删除 `@TableLogic`**。
6. DO 中字段优先使用包装类型（Long、Integer），避免基础类型 null 映射问题。

### 5.2 Mapper 编写

1. Mapper 继承基础 `BaseMapperX / BaseMapper`。
2. 动态条件优先使用 `LambdaQueryWrapper`，使用 `likeIfPresent / eqIfPresent`，null 值自动跳过条件。
3. 禁止 `select *`；禁止直接拼接 SQL 字符串，防止注入；尽量少写 XML，复杂查询才使用 XML。
4. 禁止无 where 条件的 update、delete；批量操作不要循环调用 Mapper，使用 `saveBatch` 批量方法。

---

## 六、异常与错误码规范

1. 全局统一异常处理器，拦截所有业务异常、系统异常，返回统一 `CommonResult` 格式。
2. 业务异常统一抛出：`throw ServiceExceptionUtil.exception(错误码常量, 参数)`。
3. 错误码统一常量管理，格式：`业务对象_场景`，如 `SESSION_NOT_FOUND`、`DOC_DUPLICATE`：
   - `_NOT_FOUND` 不存在
   - `_DUPLICATE` 重复
   - `_FAILED` 执行失败
   - `_BAD_PARAM` 参数非法
4. 禁止在业务代码中写硬编码错误字符串。

---

## 七、消息队列 / 异步处理通用规范

1. 消息体实现序列化接口；消息生产者发送消息；消费者 `@EventListener + @Async` 实现异步消费。
2. 耗时任务、文档解析、向量化等不要阻塞 HTTP 主线程，交给异步 / MQ。
3. MQ 消费者必须做好幂等，防止重复消费导致数据错乱。

---

## 八、定时任务通用规范

1. 禁止裸写 `@Scheduled`；统一实现任务接口，在配置 / 后台管理注册任务。
2. 任务日志完整记录执行结果、异常堆栈。

---

## 九、安全编码红线（强制）

1. 接口免登录白名单严格评审，禁止随意放开。
2. 密码、密钥、token 禁止打日志，禁止 VO 返回敏感字段；敏感数据脱敏输出。
3. 当前登录用户信息**禁止从前端请求参数获取**，从安全上下文工具类读取。
4. 密钥、密码、配置不要硬编码写死代码，使用环境变量、配置中心。
5. 防止 SQL 注入，禁止直接拼接 SQL；入参做校验。
6. 列表查询必须分页，禁止无限制全表查询，避免 OOM。

---

## 十、日志、注释、代码风格

1. 禁止 `System.out / System.err` 打印，统一使用 `@Slf4j` 日志；日志格式模板：`log.info("[方法名][描述({})]", 参数)`；异常打印完整堆栈。
2. 类写 Javadoc 说明功能；关键业务步骤写中文注释；枚举、错误码写注释。
3. 不要遗留旧版本 `_bak / _old` 废弃代码，直接删除。
4. 配置统一放在 yml 配置文件，自定义配置使用 `@ConfigurationProperties(prefix = "xxx")` 接收，禁止硬编码阈值、第三方地址。

---

## 十一、单元测试规范

1. Service 层单元测试覆盖正常流程、异常分支；断言业务异常对应的错误码。
2. 测试类命名：`XxxServiceImplTest`；测试方法命名：`test方法名_场景`，例如 `testCreateSession_nameDuplicate`。

---

## 十二、标准 CRUD 开发 Checklist（开发完成自查）

- [ ] 建表，包含通用字段 id、create_time、update_time、creator、updater、deleted；字段、表注释完整
- [ ] 定义错误码常量
- [ ] DO、ReqVO、RespVO、Mapper、Service、Controller 全套生成 / 编写
- [ ] Controller 参数校验 `@Valid`；接口文档注解齐全
- [ ] Service 层新增 / 修改前做存在性、唯一性校验
- [ ] 写操作添加 `@Transactional` 事务
- [ ] 返回统一 `CommonResult`；业务异常抛出 BizException
- [ ] 分页查询，禁止全量查询
- [ ] 单元测试覆盖正常、异常分支
