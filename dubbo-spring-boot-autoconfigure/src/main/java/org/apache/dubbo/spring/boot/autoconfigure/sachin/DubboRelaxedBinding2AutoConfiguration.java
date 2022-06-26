package org.apache.dubbo.spring.boot.autoconfigure.sachin;


import com.alibaba.spring.context.config.ConfigurationBeanBinder;
import com.alibaba.spring.util.PropertySourcesUtils;
import org.apache.dubbo.spring.boot.autoconfigure.BinderDubboConfigBinder;
import org.apache.dubbo.spring.boot.autoconfigure.DubboRelaxedBindingAutoConfiguration;
import org.apache.dubbo.spring.boot.util.DubboUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Configuration
@ConditionalOnProperty(prefix = DubboUtils.DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.boot.context.properties.bind.Binder")
@AutoConfigureBefore(DubboRelaxedBindingAutoConfiguration.class)
public class DubboRelaxedBinding2AutoConfiguration {

    public PropertyResolver dubboScanBasePackagesPropertyResolver(ConfigurableEnvironment environment) {

        ConfigurableEnvironment propertyResolver = new AbstractEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                Map<String, Object> dubboScanProperties = PropertySourcesUtils.getSubProperties(environment.getPropertySources(), DubboUtils.DUBBO_SCAN_PREFIX);
                propertySources.addLast(new MapPropertySource("dubboScanProperties", dubboScanProperties));

            }
        };
        ConfigurationPropertySources.attach(propertyResolver);
        return propertyResolver;
    }


    @ConditionalOnMissingBean(name = DubboUtils.BASE_PACKAGES_BEAN_NAME)
    @Bean(name = DubboUtils.BASE_PACKAGES_BEAN_NAME)
    public Set<String> dubboBasePackages(ConfigurableEnvironment environment) {
        PropertyResolver propertyResolver = dubboScanBasePackagesPropertyResolver(environment);

        return propertyResolver.getProperty(DubboUtils.BASE_PACKAGES_PROPERTY_NAME, Set.class, Collections.emptySet());
    }


    @ConditionalOnMissingBean(name = DubboUtils.RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME, value = ConfigurationBeanBinder.class)
    @Bean(name = DubboUtils.RELAXED_DUBBO_CONFIG_BINDER_BEAN_NAME)
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ConfigurationBeanBinder relaxedDubboConfigBinder(){
        return new BinderDubboConfigBinder();
    }
}
