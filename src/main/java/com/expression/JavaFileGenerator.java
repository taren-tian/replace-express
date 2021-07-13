package com.expression;

import java.util.ArrayList;
import java.util.List;

/**
 * create a java file by Str
 */
public class JavaFileGenerator {
    /**
     * import导入的包
     */
    private static final List<String> IMPORT_LIST;

    private final static String IMPORT = "import ";

    private static final String CLASS_HEADER = "@SuppressWarnings(\"unchecked\")\n" +
            "public class ";

    private static final String IMPLIEMENTS = " implements ";

    private static final String CLASS_TAIL = " {\n" +
            "    public Object execute(TestEntity testEntity) throws Exception {\n";

    private static final String TAIL =
            "    }\n" +
                    "}";

    static {
        //以下为默认导入的包
        //为了安全问题不可随意修改
        //或者一些可能共同用到的类，utils...
        IMPORT_LIST = new ArrayList<>();
        IMPORT_LIST.add("java.util.*");
        IMPORT_LIST.add("com.entity.*");
    }

    public static String buildJavaFile(String className, String methodBody) {

        StringBuilder java = new StringBuilder();
        List<String> import_list = new ArrayList<>();
        import_list.addAll(IMPORT_LIST);
        import_list.add("com.expression.entity.ExpressionTemplate");
        for (String importPackage : import_list) {
            java.append(IMPORT);
            java.append(importPackage);
            java.append(";\n");
        }
        java.append(CLASS_HEADER);
        java.append(className);
        java.append(IMPLIEMENTS);
        java.append("ExpressionTemplate");
        java.append(CLASS_TAIL);
        java.append(methodBody);
        java.append(TAIL);

        return java.toString();
    }

}
