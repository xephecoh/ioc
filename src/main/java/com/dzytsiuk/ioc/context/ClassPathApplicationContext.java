package com.dzytsiuk.ioc.context;


import com.dzytsiuk.ioc.context.cast.JavaNumberTypeCast;
import com.dzytsiuk.ioc.entity.Bean;
import com.dzytsiuk.ioc.entity.BeanDefinition;
import com.dzytsiuk.ioc.io.BeanDefinitionReader;
import com.dzytsiuk.ioc.io.XMLBeanDefinitionReader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ClassPathApplicationContext implements ApplicationContext {
    private static final String SETTER_PREFIX = "set";
    private static final int SETTER_PARAMETER_INDEX = 0;

    private Map<String, Bean> beans;
    private BeanDefinitionReader beanDefinitionReader;

    public ClassPathApplicationContext() {

    }

    public ClassPathApplicationContext(String... path) {
        setBeanDefinitionReader(new XMLBeanDefinitionReader(path));
        start();
    }

    public void start() {
        beans = new HashMap<>();
        List<BeanDefinition> beanDefinitions = beanDefinitionReader.getBeanDefinitions();
        instantiateBeans(beanDefinitions);
        injectValueDependencies(beanDefinitions);
        injectRefDependencies(beanDefinitions);
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        return null;
    }

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        return null;
    }

    @Override
    public <T> T getBean(String name) {
        return (T) beans.get(name).getValue();
    }

    @Override
    public void setBeanDefinitionReader(BeanDefinitionReader beanDefinitionReader) {
        this.beanDefinitionReader = beanDefinitionReader;
    }

    private void instantiateBeans(List<BeanDefinition> beanDefinitions) {
        beanDefinitions.stream()
                .map(beanDefinition -> {
                    Object object;
                    try {
                        object = Class.forName(beanDefinition.getBeanClassName()).getConstructor().newInstance();
                    } catch (InstantiationException | ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                    Bean bean = new Bean();
                    bean.setId(beanDefinition.getId());
                    bean.setValue(object);
                    return bean;
                }).forEach(bean -> beans.put(bean.getId(), bean));
    }


    private void injectValueDependencies(List<BeanDefinition> beanDefinitions) {
        beanDefinitions.forEach(beanDefinition -> {
            Bean bean = beans.get(beanDefinition.getId());
            if (bean != null) {
                if (beanDefinition.getDependencies() != null) {
                    Map<String, String> valueDependencies = beanDefinition.getDependencies();
                    for (Map.Entry<String, String> dependency : valueDependencies.entrySet()) {
                        try {
                            String fieldBeanDefinitionName = dependency.getKey();
                            String fieldBeanDefinitionValue = dependency.getValue();
                            String setterName = getSetterName(fieldBeanDefinitionName);
                            System.out.println(fieldBeanDefinitionName + setterName + fieldBeanDefinitionValue);
                            Class<?> fieldClass = bean.getValue().getClass().getDeclaredField(fieldBeanDefinitionName).getType();
                            if(fieldClass.isPrimitive()) {
                                Object fieldValue = JavaNumberTypeCast.castPrimitive(fieldBeanDefinitionValue, fieldClass);
                                if (fieldValue != null) {
                                    Method method = bean.getClass().getMethod(setterName, fieldValue.getClass());
                                    method.invoke(bean, fieldValue);
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            System.out.println("No  method inside  " + bean.getValue().getClass());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            System.out.println(e.getMessage());
                        } catch (NoSuchFieldException e) {
                            System.out.println("No field inside  " + bean.getClass());
                        }
                    }
                }
            }
        });
    }


    private void injectRefDependencies(List<BeanDefinition> beanDefinitions) {
        beanDefinitions.forEach(beanDefinition -> {
            Bean bean = beans.get(beanDefinition.getId());
            if (bean != null) {
                Map<String, String> refDependencies = beanDefinition.getRefDependencies();
                if (refDependencies != null) {
                    for (Map.Entry<String, String> dependency : refDependencies.entrySet()) {
                        try {
                            String propertyName = dependency.getKey();
                            Object referenceObject = getBean(propertyName);
                            String setterName = getSetterName(propertyName);
                            Method method = bean.getClass().getMethod(setterName, Object.class);
                            method.invoke(bean, referenceObject);
                        } catch (NoSuchMethodException e) {
                            System.out.println("No method inside class " + bean.getClass());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
        });
    }

    private String getSetterName(String propertyName) {
        return SETTER_PREFIX + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

}
