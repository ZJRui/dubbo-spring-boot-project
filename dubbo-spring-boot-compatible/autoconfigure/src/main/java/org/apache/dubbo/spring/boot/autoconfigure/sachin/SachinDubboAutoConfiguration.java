package org.apache.dubbo.spring.boot.autoconfigure.sachin;


import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceClassPostProcessor;
import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.apache.dubbo.spring.boot.autoconfigure.DubboConfigurationProperties;
import org.apache.dubbo.spring.boot.autoconfigure.DubboRelaxedBindingAutoConfiguration;
import org.apache.dubbo.spring.boot.util.DubboUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@ConditionalOnProperty(prefix = DubboUtils.DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@Configuration
@AutoConfigureAfter(DubboRelaxedBindingAutoConfiguration.class)
@EnableConfigurationProperties(DubboConfigurationProperties.class)
@EnableDubboConfig

public class SachinDubboAutoConfiguration {

    @ConditionalOnProperty(prefix = DubboUtils.DUBBO_SCAN_PREFIX, name = DubboUtils.BASE_PACKAGES_PROPERTY_NAME)
    @ConditionalOnBean(name = DubboUtils.BASE_PACKAGES_BEAN_NAME)
    @Bean
    public ServiceClassPostProcessor serviceClassPostProcessor(@Qualifier(DubboUtils.BASE_PACKAGES_BEAN_NAME )
                                                               Set<String> packageToScan){

        return new ServiceClassPostProcessor(packageToScan);
    }
}
