package com.init;

import com.expression.ExpressionCompilerUtil;
import com.expression.entity.ExpressionObjectCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.utils.CacheUtils;

import javax.servlet.ServletContext;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * cache init
 */
public class CacheInit implements Starter {

    private static final Logger logger = LoggerFactory.getLogger(CacheInit.class);

    @Override
    public void init(ServletContext ctx) {
        logger.info("初始化缓存开始");
        long startTime = System.currentTimeMillis();
        //读取需要初始化的配置
        List<Map<String, Object>> list = null;
        logger.info("缓存总数为：{}", list == null ? 0 : list.size());
        if (null != list && !list.isEmpty()) {
            Map<String, Object> cacheMap = CacheUtils.cacheMap;
            ExpressionObjectCache expressionObjectCache = ExpressionObjectCache.getObjectCache(list.size());
            int threadNum = Runtime.getRuntime().availableProcessors();
            threadNum = Double.valueOf(threadNum * 0.65).intValue();
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(threadNum);
            CountDownLatch countDownLatch = new CountDownLatch(list.size());
            for (Map<String, Object> extendMap : list) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this) {
                            String extendCode = (String) extendMap.get("extend_code");
                            String extendContent = (String) extendMap.get("extend_content");
                            logger.info("extendCode:{};extendContent:{}", extendCode, extendContent);
                            //时间可能需要处理一下
                            long extendTime = (Long) extendMap.get("gmt_create");
                            String className = ExpressionCompilerUtil.getNameByExtendCode(extendCode);
                            String complieText = doCompileText(extendCode);
                            expressionObjectCache.addCache(className, complieText);
                            if (cacheMap.containsKey(extendCode)) {
                                Map<String, Object> extendMap = (Map<String, Object>) cacheMap.get(extendCode);
                                extendMap.put("complieText", complieText);
                                //或者其他还要缓存的东西，灵活一点
                                // extendMap.put("otherKey",null);
                                cacheMap.putIfAbsent(extendCode, extendMap);
                            } else {
                                Map<String, Object> newMap = new HashMap<>();
                                newMap.put("complieText", complieText);
                                cacheMap.putIfAbsent(extendCode, newMap);
                            }
                            countDownLatch.countDown();
                        }
                    }
                };
                fixedThreadPool.execute(runnable);
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            fixedThreadPool.shutdown();
        }
        long endTime = System.currentTimeMillis();
        logger.info("初始化缓存结束，总耗时{}", (endTime - startTime));
    }

    private String doCompileText(String code) {
        String className = ExpressionCompilerUtil.getNameByExtendCode(code);
        byte[] compilerTextByte = ExpressionCompilerUtil.getCompilerTextFromMemory(className);
        return Base64.getDecoder().decode(compilerTextByte).toString();
    }
}
