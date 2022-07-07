/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.boot.dubbo.autoconfigure;

import com.alibaba.dubbo.config.AbstractConfig;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import com.alibaba.dubbo.config.spring.context.annotation.DubboConfigConfiguration;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import java.util.Set;

import static com.alibaba.boot.dubbo.util.DubboUtils.BASE_PACKAGES_PROPERTY_NAME;
import static com.alibaba.boot.dubbo.util.DubboUtils.DUBBO_PREFIX;
import static com.alibaba.boot.dubbo.util.DubboUtils.MULTIPLE_CONFIG_PROPERTY_NAME;
import static java.util.Collections.emptySet;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Dubbo Auto {@link Configuration}
 *
 * @see ApplicationConfig
 * @see Service
 * @see Reference
 * @see DubboComponentScan
 * @see EnableDubboConfig
 * @see EnableDubbo
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true, havingValue = "true")
@ConditionalOnClass(AbstractConfig.class)
public class DubboAutoConfiguration {

    /**
     * Creates {@link ServiceAnnotationBeanPostProcessor} Bean
     *
     * @param environment {@link Environment} Bean
     * @return {@link ServiceAnnotationBeanPostProcessor}
     */
    @ConditionalOnProperty(name = BASE_PACKAGES_PROPERTY_NAME)
    @ConditionalOnClass(ConfigurationPropertySources.class)
    @Bean
    public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor(Environment environment) {
        Set<String> packagesToScan = environment.getProperty(BASE_PACKAGES_PROPERTY_NAME, Set.class, emptySet());
        return new ServiceAnnotationBeanPostProcessor(packagesToScan);
    }

    @ConditionalOnClass(Binder.class)
    @Bean
    @Scope(scopeName = SCOPE_PROTOTYPE)
    public RelaxedDubboConfigBinder relaxedDubboConfigBinder() {
        return new RelaxedDubboConfigBinder();
    }

    /**
     * Creates {@link ReferenceAnnotationBeanPostProcessor} Bean if Absent
     *
     * @return {@link ReferenceAnnotationBeanPostProcessor}
     */
    @ConditionalOnMissingBean
    @Bean(name = ReferenceAnnotationBeanPostProcessor.BEAN_NAME)
    public ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor() {
        return new ReferenceAnnotationBeanPostProcessor();
    }

    /**
     * Single Dubbo Config Configuration
     *
     * @see EnableDubboConfig
     * @see DubboConfigConfiguration.Single
     */
    @EnableDubboConfig
    protected static class SingleDubboConfigConfiguration {
    }

    /**
     * Multiple Dubbo Config Configuration , equals @EnableDubboConfig.multiple() == <code>true</code>
     *
     * @see EnableDubboConfig
     * @see DubboConfigConfiguration.Multiple
     */
    /**
     * havingValue :
     * 匹配规则：属性文件中查找key（prefix+"."+name）
     *
     * 属性文件中设置了key的值value：
     *
     * ConditionalOnProperty设置了havingValue：value=havingvalue则匹配，若不等则不匹配
     *
     * ConditionalOnproperty没有设置havingValue：value不等于false则匹配，若为false，则不匹配
     *
     * 属性文件中没有设置key：
     *
     * ConditionalOnProperty中matchingIfMissing：true匹配，false不匹配
     */
    @ConditionalOnProperty(name = MULTIPLE_CONFIG_PROPERTY_NAME, havingValue = "true")
    @EnableDubboConfig(multiple = true)
    protected static class MultipleDubboConfigConfiguration {
        /**
         *
         *    Dubbo框架支持一个dubbo应用对应多个配置信息（比如多配置中心）  默认支持单配置 SingleDubboConfigConfiguration
         *
         *      如果有dubbo.config.mutilple=true则为会注册MultipleDubboConfigConfiguration  支持多注册中心主要应用场景：测试环境相关服务资源不匹配，生产环境平滑迁移。
         *
         * 这两个类主要是起到标识作用并没有实际作用（没有被bean实例化）主要是这两个类被注解@EnableDubboConfig修饰，该注解又引入了一个DubboConfigConfigurationSelector。
         *
         */
    }

}
