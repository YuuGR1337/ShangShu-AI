package com.ai.api.workflow.engine;

/** 变量不存在异常：resolveTyped 读取的变量路径在变量池中缺失时抛出，消息携带表达式便于定位节点配置。 */
public class VariableNotFoundException extends RuntimeException {

  public VariableNotFoundException(String expression) {
    super("变量不存在: " + expression);
  }
}
