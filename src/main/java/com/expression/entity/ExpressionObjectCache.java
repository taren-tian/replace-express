package com.expression.entity;

import com.expression.ExpressionCompilerUtil;
import java.util.concurrent.ConcurrentHashMap;


public class ExpressionObjectCache extends ConcurrentHashMap<String, Object> {

    private volatile static ExpressionObjectCache objectCache;

    private static final int[] LOCK = {};

    public static final Integer TIME_LENGTH = 14;

    private ExpressionObjectCache(Integer size) {
        super(size);
    }

    public static ExpressionObjectCache getObjectCache(Integer size) {
        if (objectCache == null) {
            synchronized (ExpressionObjectCache.LOCK) {
                if (objectCache == null) {
                    objectCache = new ExpressionObjectCache(size);
                }
            }
        }
        return objectCache;
    }


    /**
     * 根据数据库数据根据用户的类，将新增添加进缓存
     */
    public void addCache(String className, String compilerText) {

        Object template;
        template = ExpressionCompilerUtil.getExpressionTemplate(className, compilerText);
        objectCache.put(getCacheNameByClassName(className), template);
    }

    public Object get(String className) {
        return super.get(getCacheNameByClassName(className));
    }

    /**
     * 获取cache名称。相较于className，cache名称去除了字符串
     *
     * @param className 类名
     * @return 返回cache名称
     */
    private String getCacheNameByClassName(String className) {
        return className.substring(0, className.length() - TIME_LENGTH);
    }

    public void remove(String className) {
        super.remove(getCacheNameByClassName(className));
    }


}
