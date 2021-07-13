package com.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@WebListener
public class Initializer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(Initializer.class);

    @Autowired
    WebApplicationContext webApplicationConnect;

    private List<Object> starters = new ArrayList<>();

    public Initializer() {
        addStarter(new CacheInit());
    }

    void addStarter(Starter startup) {
        starters.add(startup);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("加载缓存");
        ServletContext ctx = sce.getServletContext();
        Iterator<Object> it = starters.iterator();
        while (it.hasNext()) {
            Starter s = (Starter) it.next();
            s.init(ctx);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
