package org.apache.dubbo.spring.boot.autoconfigure.sachin;

import com.alibaba.spring.context.config.ConfigurationBeanBinder;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;

import java.util.Map;

public class SachinRelaxedDubboConfigBinder  implements ConfigurationBeanBinder {


    @Override
    public void bind(Map<String, Object> configurationProperties, boolean ignoreUnknownFields,
                     boolean ignoreInvalidFields, Object configurationBean) {

        RelaxedDataBinder relaxedDataBinder = new RelaxedDataBinder(configurationBean);
        //set ignored
        relaxedDataBinder.setIgnoreInvalidFields(ignoreInvalidFields);

        relaxedDataBinder.setIgnoreInvalidFields(ignoreUnknownFields);

        MutablePropertyValues propertyValues=new MutablePropertyValues(configurationProperties);
        relaxedDataBinder.bind(propertyValues);

    }
}
