# 工作流节点 SPI 扩展指南

> WorkflowEngine 通过 Spring 依赖注入自动收集容器内所有 `NodeExecutor` Bean 来构建节点注册表，
> 内置 20 种节点与第三方扩展节点遵循同一注册机制，**新增节点无需修改引擎代码**。

## 引擎如何发现节点

- `WorkflowEngine` 构造器注入 `List<NodeExecutor>`，按 `type()` 返回值构建注册表（type 即前端 DSL 中节点的 `type` 字段）。
- 容器中出现重复 `type()` 时应用启动失败并明确报错，避免节点被静默覆盖。
- 本项目内置节点均以 `@Component` 注册在 `com.ai.api.workflow.engine.node` 包下。

## 第三方扩展步骤（独立 jar）

1. **实现执行器**：实现 `com.ai.api.workflow.engine.NodeExecutor` 接口：

```java
public class MyNodeExecutor implements NodeExecutor {

  @Override
  public String type() {
    return "my-node"; // 与前端 DSL 的节点 type 一致，全局唯一
  }

  @Override
  public NodeResult execute(NodeContext context) throws Exception {
    // context.nodeData() 取节点配置；context.variablePool() 读写变量
    return new NodeResult(Map.of("output", "value"), 0L, null);
  }
}
```

2. **注册为 Bean**：提供自动配置类，而不是依赖组件扫描（第三方 jar 不在主应用扫描范围内）：

```java
@AutoConfiguration
public class MyNodeAutoConfiguration {

  @Bean
  public MyNodeExecutor myNodeExecutor() {
    return new MyNodeExecutor();
  }
}
```

3. **声明自动配置**：在 jar 的 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   文件中写入自动配置类全限定名：

```
com.example.workflow.MyNodeAutoConfiguration
```

4. **引入依赖**：主应用添加该 jar 依赖后重启，`my-node` 类型即生效，SSE 事件、执行记录、变量池与内置节点行为一致。

## 前端配套

节点类型要在画布中使用还需前端配套（见 `ai-agent-web/AGENTS.md` 工作流节点开发约定）：

1. `src/views/workflow/nodes/` 下新增 `MyNode.vue`（继承 `BaseNode.vue` 结构）
2. `NodeConfigPanel.vue` 增加对应配置表单
3. `NodePanel.vue` 增加拖拽项
