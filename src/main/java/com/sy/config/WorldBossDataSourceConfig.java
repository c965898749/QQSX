package com.sy.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 双数据源装配：
 * <p>主库(192.168.0.106) 的 DataSource 仍由 Druid/MyBatis-Plus 自动配置提供（com.sy.mapper.* 走它）；
 * 世界BOSS库(10.126.126.1) 作为第二数据源，其 DataSource 以内联方式构建（不注册为 Bean，
 * 避免触发 Druid 自动配置的 @ConditionalOnMissingBean(DataSource) 回退），并绑定到
 * com.sy.worldboss.mapper 的独立 SqlSessionFactory。
 * <p>注意：由于新增了第二个 SqlSessionFactory，MyBatis-Plus 自动配置的主 SqlSessionFactory 会回退，
 * 因此这里手动重建主库 SqlSessionFactory（masterSqlSessionFactory，@Primary），并忠实复刻
 * application.properties 里 mybatis-plus.* 的关键设置。
 */
@Configuration
@MapperScan(basePackages = "com.sy.worldboss.mapper", sqlSessionFactoryRef = "worldbossSqlSessionFactory")
public class WorldBossDataSourceConfig {

    // ===== 主库类型别名 / mapper xml 位置（与 mybatis-plus.* 保持一致） =====
    private static final String MASTER_MAPPER_LOCATIONS = "classpath:mapper/*.xml";
    private static final String WORLDBOSS_MAPPER_LOCATIONS = "classpath:mapper-worldboss/*.xml";
    private static final String TYPE_ALIASES_PACKAGE = "com.sy.entity,com.sy.model";

    @Value("${worldboss.datasource.driver-class-name}")
    private String wbDriverClassName;
    @Value("${worldboss.datasource.url}")
    private String wbUrl;
    @Value("${worldboss.datasource.username}")
    private String wbUsername;
    @Value("${worldboss.datasource.password}")
    private String wbPassword;
    @Value("${worldboss.datasource.initial-size:5}")
    private int wbInitialSize;
    @Value("${worldboss.datasource.min-idle:5}")
    private int wbMinIdle;
    @Value("${worldboss.datasource.max-active:20}")
    private int wbMaxActive;
    @Value("${worldboss.datasource.test-on-borrow:true}")
    private boolean wbTestOnBorrow;
    @Value("${worldboss.datasource.test-on-return:false}")
    private boolean wbTestOnReturn;
    @Value("${worldboss.datasource.test-while-idle:true}")
    private boolean wbTestWhileIdle;
    @Value("${worldboss.datasource.time-between-eviction-runs-millis:60000}")
    private long wbTimeBetweenEvictionRunsMillis;
    @Value("${worldboss.datasource.min-evictable-idle-time-millis:300000}")
    private long wbMinEvictableIdleTimeMillis;
    @Value("${worldboss.datasource.validation-query:SELECT 1}")
    private String wbValidationQuery;

    /**
     * 主库 SqlSessionFactory（@Primary），消费自动配置出来的 Druid DataSource。
     */
    @Bean(name = "masterSqlSessionFactory")
    @Primary
    public SqlSessionFactory masterSqlSessionFactory(DataSource dataSource,
                                                     MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTypeAliasesPackage(TYPE_ALIASES_PACKAGE);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(MASTER_MAPPER_LOCATIONS));
        bean.setConfiguration(buildMybatisConfiguration());
        bean.setGlobalConfig(buildGlobalConfig());
        bean.setPlugins(new Interceptor[]{mybatisPlusInterceptor});
        return bean.getObject();
    }

    /**
     * 世界BOSS库 SqlSessionFactory，绑定内联构建的第二数据源。
     */
    @Bean(name = "worldbossSqlSessionFactory")
    public SqlSessionFactory worldbossSqlSessionFactory(MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(buildWorldbossDataSource());
        bean.setTypeAliasesPackage(TYPE_ALIASES_PACKAGE);
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(WORLDBOSS_MAPPER_LOCATIONS);
        if (resources.length > 0) {
            bean.setMapperLocations(resources);
        }
        bean.setConfiguration(buildMybatisConfiguration());
        bean.setGlobalConfig(buildGlobalConfig());
        bean.setPlugins(new Interceptor[]{mybatisPlusInterceptor});
        return bean.getObject();
    }

    private MybatisConfiguration buildMybatisConfiguration() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(StdOutImpl.class);
        return configuration;
    }

    private GlobalConfig buildGlobalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        return globalConfig;
    }

    private DataSource buildWorldbossDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(wbDriverClassName);
        ds.setUrl(wbUrl);
        ds.setUsername(wbUsername);
        ds.setPassword(wbPassword);
        ds.setInitialSize(wbInitialSize);
        ds.setMinIdle(wbMinIdle);
        ds.setMaxActive(wbMaxActive);
        ds.setTestOnBorrow(wbTestOnBorrow);
        ds.setTestOnReturn(wbTestOnReturn);
        ds.setTestWhileIdle(wbTestWhileIdle);
        ds.setTimeBetweenEvictionRunsMillis(wbTimeBetweenEvictionRunsMillis);
        ds.setMinEvictableIdleTimeMillis(wbMinEvictableIdleTimeMillis);
        ds.setValidationQuery(wbValidationQuery);
        return ds;
    }
}
