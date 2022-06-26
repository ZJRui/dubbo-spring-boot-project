package org.apache.dubbo.spring.boot.autoconfigure.sachin;

import com.alibaba.spring.context.config.ConfigurationBeanBinder;
import org.apache.dubbo.spring.boot.autoconfigure.RelaxedDubboConfigBinder;
import org.apache.dubbo.spring.boot.util.DubboUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.util.Set;

import static java.util.Collections.emptySet;
import static org.apache.dubbo.spring.boot.util.DubboUtils.*;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.boot.bind.RelaxedPropertyResolver")
@Configuration
@SuppressWarnings("all")
public class SachinDubboRelaxedBindingAutoConfiguration {

    public PropertyResolver dubboScanBasePackagePropertyResolver(Environment environment) {

        /**
         * RelaxedPropertyResolver的作用： 从Environment中解析得到指定前缀的属性
         * 凡是被Spring管理的类，实现接口 EnvironmentAware 重写方法 setEnvironment 可以在工程启动时，
         * 获取到系统环境变量和application配置文件中的变量。
         *   propertyResolver = new RelaxedPropertyResolver(env, "spring.datasource." );
         *          String url = propertyResolver.getProperty( "url" );
         */
        return new RelaxedPropertyResolver(environment, DubboUtils.DUBBO_SCAN_PREFIX);
    }

    /**
     * 从Environment中解析得到 base_package 属性配置
     * @param environment
     * @return
     */
    @ConditionalOnMissingBean(name = BASE_PACKAGES_BEAN_NAME)
    @Bean(name = BASE_PACKAGES_BEAN_NAME)
    public Set<String> dubboBasePackages(Environment environment) {
        PropertyResolver propertyResolver = dubboScanBasePackagePropertyResolver(environment);
        return propertyResolver.getProperty(BASE_PACKAGES_PROPERTY_NAME, Set.class, emptySet());
    }

    @ConditionalOnMissingBean(name = RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME,
            value = ConfigurationBeanBinder.class)
    @Bean(RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME)
    @Scope(scopeName = SCOPE_PROTOTYPE)
    public ConfigurationBeanBinder relaxedDubboConfigBinder() {
        return new RelaxedDubboConfigBinder();
    }







}
