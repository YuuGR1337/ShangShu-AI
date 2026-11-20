package com.ai.api.data.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * <p>依据《企业Java后端开发规范.md》与 design.md D6：
 *
 * <ul>
 *   <li>启用分页插件 {@link PaginationInnerInterceptor}（默认 MySQL 方言）
 *   <li>启用乐观锁插件 {@link OptimisticLockerInnerInterceptor}
 *   <li>所有 Mapper 必须继承 BaseMapper&lt;T&gt;，复杂查询写在 XML
 * </ul>
 *
 * @author bootstrap-project
 */
@Configuration
public class MybatisPlusConfig {

  /**
   * 注册 MyBatis-Plus 拦截器链。
   *
   * @return MybatisPlusInterceptor
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 分页插件：如果未指定 DbType 则自动识别
    PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
    // 单页最大 500 条，防止恶意拉取全表
    paginationInterceptor.setMaxLimit(500L);
    // 乐观锁插件：配合 @Version 注解使用
    OptimisticLockerInnerInterceptor optimisticLockerInterceptor =
        new OptimisticLockerInnerInterceptor();
    interceptor.addInnerInterceptor(paginationInterceptor);
    interceptor.addInnerInterceptor(optimisticLockerInterceptor);
    return interceptor;
  }
}
