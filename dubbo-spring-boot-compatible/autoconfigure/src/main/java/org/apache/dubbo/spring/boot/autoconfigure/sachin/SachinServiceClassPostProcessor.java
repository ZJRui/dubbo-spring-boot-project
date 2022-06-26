package org.apache.dubbo.spring.boot.autoconfigure.sachin;

import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.beans.factory.annotation.AnnotationPropertyValuesAdapter;
import org.apache.dubbo.config.spring.beans.factory.annotation.ServiceBeanNameBuilder;
import org.apache.dubbo.config.spring.context.DubboBootstrapApplicationListener;
import org.apache.dubbo.config.spring.context.annotation.DubboClassPathBeanDefinitionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.ObjectUtils.of;
import static org.springframework.util.ClassUtils.getAllInterfacesForClass;
import static org.springframework.util.ClassUtils.resolveClassName;

public class SachinServiceClassPostProcessor implements BeanDefinitionRegistryPostProcessor,
        ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

    private final static List<Class<? extends Annotation>> serviceAnnotationTypes = Arrays.asList(
            DubboService.class,
            Service.class,
            com.alibaba.dubbo.config.annotation.Service.class
    );
    private Logger logger = LoggerFactory.getLogger(getClass());
    protected final Set<String> packageToScan;

    private Environment environment;
    private ResourceLoader resourceLoader;
    private ClassLoader classLoader;

    public SachinServiceClassPostProcessor(Collection<String> packageToScan) {
        this(new LinkedHashSet<>(packageToScan));
    }

    public SachinServiceClassPostProcessor(Set<String> packageToScan) {
        this.packageToScan = packageToScan;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        // 注册applicationListener
        registerInfrastructureBean(beanDefinitionRegistry, DubboBootstrapApplicationListener.BEAN_NAME, DubboBootstrapApplicationListener.class);
        Set<String> resolvedPackageToScan = resolvePackagesToScan(packageToScan);
        if (!CollectionUtils.isEmpty(resolvedPackageToScan)) {
            registerServiceBean(resolvedPackageToScan, beanDefinitionRegistry);
        }

    }

    private void registerServiceBean(Set<String> packageToScan, BeanDefinitionRegistry registry) {
        DubboClassPathBeanDefinitionScanner scanner = new DubboClassPathBeanDefinitionScanner(registry,
                environment, resourceLoader);
        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);
        scanner.setBeanNameGenerator(beanNameGenerator);
        serviceAnnotationTypes.forEach(annotationType -> {
            /**
             * TypeFilter :
             * 遍历找到的Resource集合，通过includeFilters和excludeFilters判断是否解析。这里的includeFilters和excludeFilters
             * 是TypeFilter接口类型的集合，是ClassPathBeanDefinitionScanner内部的属性。
             * TypeFilter接口是一个用于判断类型是否满足要求的类型过滤器。excludeFilters中只
             * 要有一个TypeFilter满足条件，这个Resource就会被过滤。includeFilters中只要有一个
             * TypeFilter满足条件，这个Resource就不会被过滤
             * TypeFilter接口目前有AnnotationTypeFilter实现类(类是否有注解修饰)、
             * RegexPatternTypeFilter(类名是否满足正则表达式)等。
             *
             */
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));

        });
        for (String packageToScanItem : packageToScan) {
            // register @Service bean first
            scanner.scan(packageToScanItem);

            // find all beanDefinitionHolder of @Service
            Set<BeanDefinitionHolder> serviceBeanDefinitionHolders = findServiceBeanDefinitionHolders(scanner, packageToScanItem, registry, beanNameGenerator);
            if (!CollectionUtils.isEmpty(serviceBeanDefinitionHolders)) {
                for (BeanDefinitionHolder serviceBeanDefinitionHolder : serviceBeanDefinitionHolders) {

                    registerServiceBean(serviceBeanDefinitionHolder, registry, scanner);
                }


            }


        }

    }

    private void registerServiceBean(BeanDefinitionHolder serviceBeanDefinitionHolder, BeanDefinitionRegistry registry, DubboClassPathBeanDefinitionScanner scanner) {
        Class<?> beanClass = resolveClass(serviceBeanDefinitionHolder);
        Annotation serviceAnnotation = findServiceAnnotation(beanClass);
        AnnotationAttributes annotationAttributes = AnnotationUtils.getAnnotationAttributes(serviceAnnotation, false, false);
        Class<?> interfaceClass = resolveServiceInterfaceClass(annotationAttributes, beanClass);
        String annotatedServiceBeanName = serviceBeanDefinitionHolder.getBeanName();

        String beanName = generateServiceBeaName(annotationAttributes, interfaceClass);
        AbstractBeanDefinition serviceBeanDefinition = buildServiceBeanDefinition(beanName, serviceAnnotation, annotationAttributes,
                interfaceClass, annotatedServiceBeanName);
        if (scanner.checkCandidate(beanName, serviceBeanDefinition)) {
            registry.registerBeanDefinition(beanName, serviceBeanDefinition);
        }


    }

    private AbstractBeanDefinition buildServiceBeanDefinition(String beanName, Annotation serviceAnnotation, AnnotationAttributes annotationAttributes, Class<?> interfaceClass, String annotatedServiceBeanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServiceBean.class);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        String[] ignoreAttributeNames = of("provider", "monitor", "application", "module", "registry", "protocol",
                "interface", "interfaceName", "parameters");
        propertyValues.addPropertyValues(new AnnotationPropertyValuesAdapter(serviceAnnotation, environment, ignoreAttributeNames));

        builder.addPropertyValue("id", beanName);
        addPropertyReference(builder, "ref", annotatedServiceBeanName);
        builder.addPropertyValue("interface", interfaceClass.getName());
        builder.addPropertyValue("parameters", converParameters(annotationAttributes.getStringArray("parameters")));
        List<MethodConfig> methodConfigs = convertMethodConfigs(annotationAttributes.get("methods"));
        AnnotationAttributes serviceAnnotationAttributes = annotationAttributes;

        /**
         * Add {@link org.apache.dubbo.config.ProviderConfig} Bean reference
         */
        String providerConfigBeanName = serviceAnnotationAttributes.getString("provider");
        if (StringUtils.hasText(providerConfigBeanName)) {
            addPropertyReference(builder, "provider", providerConfigBeanName);
        }

        /**
         * Add {@link org.apache.dubbo.config.MonitorConfig} Bean reference
         */
        String monitorConfigBeanName = serviceAnnotationAttributes.getString("monitor");
        if (StringUtils.hasText(monitorConfigBeanName)) {
            addPropertyReference(builder, "monitor", monitorConfigBeanName);
        }

        /**
         * Add {@link org.apache.dubbo.config.ApplicationConfig} Bean reference
         */
        String applicationConfigBeanName = serviceAnnotationAttributes.getString("application");
        if (StringUtils.hasText(applicationConfigBeanName)) {
            addPropertyReference(builder, "application", applicationConfigBeanName);
        }

        /**
         * Add {@link org.apache.dubbo.config.ModuleConfig} Bean reference
         */
        String moduleConfigBeanName = serviceAnnotationAttributes.getString("module");
        if (StringUtils.hasText(moduleConfigBeanName)) {
            addPropertyReference(builder, "module", moduleConfigBeanName);
        }


        /**
         * Add {@link org.apache.dubbo.config.RegistryConfig} Bean reference
         */
        String[] registryConfigBeanNames = serviceAnnotationAttributes.getStringArray("registry");

        List<RuntimeBeanReference> registryRuntimeBeanReferences = toRuntimeBeanReferences(registryConfigBeanNames);

        if (!registryRuntimeBeanReferences.isEmpty()) {
            builder.addPropertyValue("registries", registryRuntimeBeanReferences);
        }

        /**
         * Add {@link org.apache.dubbo.config.ProtocolConfig} Bean reference
         */
        String[] protocolConfigBeanNames = serviceAnnotationAttributes.getStringArray("protocol");

        List<RuntimeBeanReference> protocolRuntimeBeanReferences = toRuntimeBeanReferences(protocolConfigBeanNames);

        if (!protocolRuntimeBeanReferences.isEmpty()) {
            builder.addPropertyValue("protocols", protocolRuntimeBeanReferences);
        }

        return builder.getBeanDefinition();

    }

    private List<RuntimeBeanReference> toRuntimeBeanReferences(String[] registryConfigBeanNames) {
        return Collections.EMPTY_LIST;
    }

    private List<MethodConfig> convertMethodConfigs(Object methods) {
        if (methods == null) {
            return Collections.EMPTY_LIST;
        }
        return MethodConfig.constructMethodConfig((Method[]) methods);
    }

    private Map<String, String> converParameters(String[] parameters) {
        return new HashMap<>();
    }

    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName,
                                      String beanName) {
        String resolvedBeanName = environment.resolvePlaceholders(beanName);
        builder.addPropertyReference(propertyName, resolvedBeanName);
    }

    private String generateServiceBeaName(AnnotationAttributes annotationAttributes, Class<?> interfaceClass) {
        ServiceBeanNameBuilder serviceBeanNameBuilder = create(interfaceClass, environment);
        ServiceBeanNameBuilder builder = serviceBeanNameBuilder.group(annotationAttributes.getString("group"))
                .version(annotationAttributes.getString("version"));
        return builder.build();


    }

    public static ServiceBeanNameBuilder create(Class<?> interfaceClass, Environment environment) {
        return  ServiceBeanNameBuilder.create(interfaceClass, environment);
    }

    private Class<?> resolveServiceInterfaceClass(AnnotationAttributes annotationAttributes, Class<?> defaultInterfaceeClass) {
        ClassLoader classLoader = defaultInterfaceeClass != null ? defaultInterfaceeClass.getClassLoader() :
                Thread.currentThread().getContextClassLoader();
        Class<?> interfaceClass = getAttribute(annotationAttributes, "interfaceClass");
        if (Void.class.equals(interfaceClass)) {
            interfaceClass = null;
            String interfaceClassName = getAttribute(annotationAttributes, "interfaceName");
            if (StringUtils.hasText(interfaceClassName)) {
                // 判断指定的类是否可以用指定的classloader加载
                if (ClassUtils.isPresent(interfaceClassName, classLoader)) {
                    interfaceClass = resolveClassName(interfaceClassName, classLoader);

                }
            }
        }
        if (interfaceClass == null && defaultInterfaceeClass != null) {
            Class<?>[] allInterfacesForClass = getAllInterfacesForClass(defaultInterfaceeClass);
            if (allInterfacesForClass.length > 0) {
                interfaceClass = allInterfacesForClass[0];
            }

        }
        return interfaceClass;
    }

    private Annotation findServiceAnnotation(Class<?> beanClass) {

        /**
         * // 在element上查询annotationType类型注解
         * // 将查询出的多个annotationType类型注解属性合并到查询的第一个注解中
         * // # 多个相同注解合并
         * org.springframework.core.annotation.AnnotatedElementUtils#findMergedAnnotation(AnnotatedElement element, Class<A> annotationType)
         */
        return serviceAnnotationTypes.stream().map(annotationType ->
                        AnnotatedElementUtils.findMergedAnnotation(beanClass, annotationType)).filter(Objects::nonNull)
                .findFirst().orElse(null)
                ;
    }


    private Class<?> resolveClass(BeanDefinitionHolder serviceBeanDefinitionHolder) {
        BeanDefinition beanDefinition = serviceBeanDefinitionHolder.getBeanDefinition();
        return resolveClass(beanDefinition);
    }

    private Class<?> resolveClass(BeanDefinition beanDefinition) {
        String beanClassName = beanDefinition.getBeanClassName();
        return resolveClassName(beanClassName, classLoader);
    }


    private Set<BeanDefinitionHolder> findServiceBeanDefinitionHolders(ClassPathBeanDefinitionScanner
                                                                               scanner, String packagerToScan,
                                                                       BeanDefinitionRegistry registry,
                                                                       BeanNameGenerator beanNameGenerator) {

        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(packagerToScan);
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(candidateComponents.size());
        for (BeanDefinition beanDefinition : candidateComponents) {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        }
        return beanDefinitionHolders;
    }

    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = null;
        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry
                    .getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
            if (beanNameGenerator == null) {
                beanNameGenerator = new AnnotationBeanNameGenerator();
            }

        }
        return beanNameGenerator;
    }

    private Set<String> resolvePackagesToScan(Set<String> packageToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<>(packageToScan.size());
        for (String packageToScanItem : packageToScan) {
            if (StringUtils.hasText(packageToScanItem)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(packageToScanItem);
                resolvedPackagesToScan.add(resolvedPackageToScan);

            }
        }
        return resolvedPackagesToScan;
    }

    public static boolean registerInfrastructureBean(BeanDefinitionRegistry beanDefinitionRegistry,
                                                     String beanName, Class<?> beanType) {
        boolean registered = false;
        if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

            beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
            registered = true;
        }

        return registered;
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {

    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {

    }
}
