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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.spring.context.properties.DubboConfigBinder;

import com.alibaba.spring.context.config.ConfigurationBeanBinder;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Map;

import static java.util.Arrays.asList;
import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.from;

/**
 * Spring Boot Relaxed {@link DubboConfigBinder} implementation
 * see org.springframework.boot.context.properties.ConfigurationPropertiesBinder
 *
 * @since 2.7.0
 */

/**
 *
 * 不加修饰符的时候 即直接声明 class A{ }
 *
 * 在这种情况下，class前面没有加任何的访问修饰符，通常称为“默认访问模式”，
 * 在该模式下，这个类只能被同一个包中的类访问或引用，这一访问特性又称包访问性。
 *
 * BinderDubboConfigBinder类如果不加public 那么 sachin 包下的DubboAutoConfiguration 就无法访问
 *
 */
public class BinderDubboConfigBinder implements ConfigurationBeanBinder {

    @Override
    public void bind(Map<String, Object> configurationProperties, boolean ignoreUnknownFields,
                     boolean ignoreInvalidFields, Object configurationBean) {

        Iterable<PropertySource<?>> propertySources = asList(new MapPropertySource("internal", configurationProperties));

        // Converts ConfigurationPropertySources
        Iterable<ConfigurationPropertySource> configurationPropertySources = from(propertySources);

        // Wrap Bindable from DubboConfig instance
        Bindable bindable = Bindable.ofInstance(configurationBean);

        Binder binder = new Binder(configurationPropertySources, new PropertySourcesPlaceholdersResolver(propertySources));

        // Get BindHandler
        BindHandler bindHandler = getBindHandler(ignoreUnknownFields, ignoreInvalidFields);

        // Bind
        binder.bind("", bindable, bindHandler);
    }

    private BindHandler getBindHandler(boolean ignoreUnknownFields,
                                       boolean ignoreInvalidFields) {
        BindHandler handler = BindHandler.DEFAULT;
        if (ignoreInvalidFields) {
            handler = new IgnoreErrorsBindHandler(handler);
        }
        if (!ignoreUnknownFields) {
            UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
            handler = new NoUnboundElementsBindHandler(handler, filter);
        }
        return handler;
    }
}
