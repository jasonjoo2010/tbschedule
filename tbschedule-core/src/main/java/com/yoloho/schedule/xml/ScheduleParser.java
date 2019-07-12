package com.yoloho.schedule.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.yoloho.schedule.config.EnableScheduleConfiguration;

public class ScheduleParser extends AbstractSingleBeanDefinitionParser {
    public static class EmptyBean {
    }
    
    @Override
    protected Class<?> getBeanClass(Element element) {
        return EmptyBean.class;
    }
    
    @Override
    protected boolean shouldGenerateId() {
        return true;
    }
    
    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        EnableScheduleConfiguration.injectInitializerBean(parserContext.getRegistry(), 
                element.getAttribute("address"), 
                element.getAttribute("root-path"), 
                element.getAttribute("username"), 
                element.getAttribute("password"));
    }
}