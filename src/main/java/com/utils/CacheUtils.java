package com.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * get a cache map
 * @author tianliangyu
 */
public class CacheUtils {

    public static Map<String, Object> cacheMap = new ConcurrentHashMap<>();

}
