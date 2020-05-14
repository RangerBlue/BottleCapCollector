package com.km.BottleCapCollector.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class CustomApplicationListener implements ApplicationListener<ApplicationEvent> {
    private static final Logger logger = LoggerFactory.getLogger(CustomApplicationListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        logger.info("Event ::" + applicationEvent.toString());
    }
}
