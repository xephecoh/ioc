package com.dzytsiuk.ioc.context;


import com.dzytsiuk.ioc.context.cast.JavaNumberTypeCast;
import com.dzytsiuk.ioc.entity.Bean;
import com.dzytsiuk.ioc.entity.BeanDefinition;
import com.dzytsiuk.ioc.exception.BeanInstantiationException;
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
        return
                (T)beans.entrySet()
                        .stream()
                        .filter(bean -> bean.getValue().getClass().equals(clazz))
                        .findAny()
                        .orElseGet(null);
    }

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        return
                (T) beans.entrySet()
                        .stream()
                        .filter(bean -> bean.getValue().getId().equals(name))
                        .filter(beans -> beans.getValue().getClass().equals(clazz))
                        .findAny()
                        .orElseGet(null);
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
                .map(this::getBeanFromBeanDefinition)
                .forEach(bean -> beans.put(bean.getId(), bean));
    }

    private Bean getBeanFromBeanDefinition(BeanDefinition beanDefinition) {
        try {
            Object object;
            object = Class.forName(beanDefinition.getBeanClassName()).getConstructor().newInstance();
            Bean bean = new Bean();
            bean.setId(beanDefinition.getId());
            bean.setValue(object);
            return bean;
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new BeanInstantiationException("Unable to instantiate bean with name " + beanDefinition.getId(), e);
        }
    }


    private void injectValueDependencies(List<BeanDefinition> beanDefinitions) {
        beanDefinitions.forEach(this::injectValDependency);
    }

    private void injectValDependency(BeanDefinition beanDefinition) {
        Bean bean = beans.get(beanDefinition.getId());
        if (bean == null) {
            return;
        }
        if (beanDefinition.getDependencies() == null) {
            return;
        }
        Map<String, String> valueDependencies = beanDefinition.getDependencies();
        if (valueDependencies == null) {
            return;
        }
        valueDependencies.forEach((k, v) -> {
            try {
                String setterName = getSetterName(k);
                Object beanValue = bean.getValue();
                Class<?> clazz = beanValue.getClass();
                Field field = clazz.getDeclaredField(k);
                Class<?> type = field.getType();
                if (type.isPrimitive()) {
                    Object fieldValue = JavaNumberTypeCast.castPrimitive(v, type);
                    if (fieldValue != null) {
                        Method method = clazz.getMethod(setterName, type);
                        method.invoke(beanValue, fieldValue);
                    }
                }
            } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new BeanInstantiationException("Unable to inject dependency to bean " + bean.getId(), e);
            }
        });
    }


    private void injectRefDependencies(List<BeanDefinition> beanDefinitions) {
        injectRefDependency(beanDefinitions);
    }

    private void injectRefDependency(List<BeanDefinition> beanDefinitions) {
        beanDefinitions.forEach(beanDefinition -> {
            Bean bean = beans.get(beanDefinition.getId());
            if (bean == null) {
                return;
            }
            Map<String, String> refDependencies = beanDefinition.getRefDependencies();
            if (refDependencies == null) {
                return;
            }
            refDependencies.forEach((propertyName, value) -> {
                try {
                    Class<?> refObjectType = bean.getValue().getClass().getDeclaredField(value).getType();
                    Object referencedObject = getBean(propertyName);
                    String setterName = getSetterName(propertyName);
                    Method method = bean.getValue().getClass().getMethod(setterName, refObjectType);
                    method.invoke(bean.getValue(), referencedObject);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
                    throw new BeanInstantiationException("Unable to inject dependency to bean " + bean.getId(), e);
                }
            });
        });
    }

    private String getSetterName(String propertyName) {
        return SETTER_PREFIX + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

}
