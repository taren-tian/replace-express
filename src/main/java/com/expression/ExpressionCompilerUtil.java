package com.expression;

import com.entity.TestEntity;
import com.exception.BizException;
import com.expression.compiler.MemoryClassLoader;
import com.expression.entity.ExpressionTemplate;
import com.pmd.StringInputPMDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import com.utils.SpringUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态编译
 */
@Component
public class ExpressionCompilerUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExpressionCompilerUtil.class);
    // 可能会用到的拼接className前缀的一些特殊符号
    private static final String CLASS_PRE = "C";

    private static final String DOLLAR_SIGN = "$";

    private static Map<String, String> tempValMap = new ConcurrentHashMap<>();

    /**
     * 编译已经存在的类，需要删除已经存在的类
     *
     * @param className class name
     * @param ruleInfo  java code
     */
    public static void compiler(String className, String ruleInfo) {
        String javaFile = JavaFileGenerator.buildJavaFile(className, ruleInfo);
        try {
            StringInputPMDUtil.checkJavaFile(className, javaFile);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            logger.error("违规java代码：{}", msg);
            throw new BizException("999999", "违规java代码： \n" + msg);
        }
        Map<String, byte[]> classBytes = MemoryClassLoader.compile(className, javaFile);
        byte[] bytes = classBytes.get(className);
        String compileText = Base64.getEncoder().encodeToString(bytes);
        tempValMap.put(className, compileText);
    }

    /**
     * 根据类名执行
     *
     * @param template 对象名，执行解析的表达式对象
     * @param object   需要处理的模型entity
     * @return 返回执行的结果
     */
    public static Object execute(ExpressionTemplate template, TestEntity object) throws Exception {
        Object result = template.execute(object);
        return result;
    }


    public static String getNameByExtendCodeAndTime(String code, LocalDateTime updateTime) {

        StringBuilder name = new StringBuilder(CLASS_PRE);

        name.append(code);
        name.append(DOLLAR_SIGN);
        name.append(updateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());

        return name.toString();
    }

    public static String getNameByExtendCode(String extendCode) {
        StringBuilder name = new StringBuilder(CLASS_PRE);

        name.append(extendCode);
        //可以拼接一些其他用来使类唯一的字段
//        name.append(DOLLAR_SIGN);
        
        return name.toString();
    }

    public static byte[] getCompilerTextFromMemory(String name) {
        String compileText = tempValMap.get(name);
        return Base64.getDecoder().decode(compileText);
    }

    public static Object getExpressionTemplate(String className, @Nullable String path, @Nullable String compilerText) {
        try {
            if (path == null && compilerText == null) {
                compilerText = tempValMap.get(className);
            }
            Class clazz = MemoryClassLoader.getDefaultLoader().loadClass(className, path, compilerText);
            String beanName = (new StringBuilder()).append(Character.toLowerCase(className.charAt(0))).append(className.substring(1)).toString();
            if (!SpringUtil.containsBean(beanName)) {
                SpringUtil.buildBean(beanName, clazz);
            }
            return SpringUtil.getBean(beanName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getExpressionTemplate(String className, @Nullable String compilerText) {
        return getExpressionTemplate(className, null, compilerText);
    }

    public static Object getExpressionTemplate(String className) {
        return getExpressionTemplate(className, null, null);
    }

}
