package com.ai.api.workflow.engine;

/** 变量类型不匹配异常：resolveTyped 读取到的实际值类型与期望类型不符时抛出。 */
public class VariableTypeMismatchException extends RuntimeException {

  public VariableTypeMismatchException(String expression, Class<?> actual, Class<?> expected) {
    super("变量类型不匹配: " + expression + " 实际为 " + actual.getName() + " 期望为 " + expected.getName());
  }
}
