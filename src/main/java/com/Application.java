package com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.WebApplicationInitializer;

@SpringBootApplication(scanBasePackages = {"com"})
@ServletComponentScan
public class Application extends SpringBootServletInitializer implements WebApplicationInitializer {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            SpringApplication springApplication = new SpringApplication(Application.class);
            ApplicationContext applicationContext = springApplication.run(args);
            if (applicationContext == null) {
                logger.error("**********************<启动失败>****************");
            } else {
                logger.info("**********************启动成功:****************");
            }
        } catch (Exception e) {
            logger.error("**********************<启动失败>****************", e);
        }
    }

}
