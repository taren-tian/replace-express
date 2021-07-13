package com.controller;

import com.entity.TestEntity;
import com.exception.BizException;
import com.expression.ExpressionCompilerUtil;
import com.expression.JavaFileGenerator;
import com.expression.entity.ExpressionTemplate;
import com.response.R;
import com.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/process")
public class ProcessController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);

    /**
     * url: localhost:8080/system/process/test1
     * <p>
     * methodBody:
     * testEntity.setVar1("var1");
     * testEntity.setVar2("var2");
     * testEntity.setVar3("var3");
     * testEntity.setInt1(1+1);
     * return testEntity;
     */
    @RequestMapping("/test1")
    public R test1(@RequestBody(required = false) Map<String, Object> params) {
        String methodBody = "testEntity.setVar1(\"var1\");\n" +
                "        testEntity.setVar2(\"var2\");\n" +
                "        testEntity.setVar3(\"var3\");\n" +
                "        testEntity.setInt1(1+1);\n" +
                "\t\treturn testEntity; ";

        TestEntity testEntity = new TestEntity();
        testEntity.setVar1("0");
        testEntity.setVar2("0");
        testEntity.setVar3("0");
        testEntity.setInt1(100);
        System.out.println("old:" + testEntity.toString());
        System.out.println("java class:" + JavaFileGenerator.buildJavaFile("codeClass", methodBody));
        System.out.println("result:" + getRunResult(testEntity, "codeClass", methodBody));
        System.out.println("new:" + testEntity.toString());
        return R.SUCCESS().put("newEntity", JsonUtils.toJSONString(testEntity));
    }

    public static Object getRunResult(TestEntity testEntity, String code, String ruleInfo) {
        String className = ExpressionCompilerUtil.getNameByExtendCode(code);

        logger.info("开始编译数据");
        try {
            ExpressionCompilerUtil.compiler(className, ruleInfo);
        } catch (BizException e) {
            throw new BizException("999992", "表达式编译异常：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("999992", "表达式编译异常：" + e.getMessage());
        }

        logger.info("开始获取执行对象");
        Object et;
        try {
            et = ExpressionCompilerUtil.getExpressionTemplate(className);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("999993", "获取执行对象异常：" + e.getMessage());
        }

        logger.info("执行操作");
        Object o = null;
        try {
            o = ExpressionCompilerUtil.execute((ExpressionTemplate) et, testEntity);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("999994", "执行异常：" + e.getMessage());
        }
        return o;
    }

}
