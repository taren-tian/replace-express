package com.pmd;

import com.exception.BizException;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * check java code whether legal
 */
public class StringInputPMDUtil {
    private static final Logger logger = LoggerFactory.getLogger(StringInputPMDUtil.class);

    public static void checkJavaFile(String className, String javaFile) {
        StringInputPMDConfiguration configuration = new StringInputPMDConfiguration();
        //TODO: move configuration settings to config file

        LanguageVersion languageVersion = LanguageRegistry.findLanguageVersionByTerseName("java" + ' ' + "1.8");
        configuration.setRuleSets("default.xml");
        configuration.setDefaultLanguageVersion(languageVersion);
        configuration.setReportFormat("text");
        configuration.setInputContentName(className);
        configuration.setInputContent(javaFile);
        Map<String, List<RuleViolation>> ruleMap = StringInputPMD.doStringInputPMD(configuration);
        if (ruleMap.size() > 0) {
            List<RuleViolation> ruleList = ruleMap.get(className);
            String errorMsg = "";
            for (RuleViolation rv : ruleList) {
                errorMsg = errorMsg + "line: " + (rv.getBeginLine() - 8) + " :: ";
                errorMsg = errorMsg + rv.getDescription() + ";" + "\n";
            }
            logger.error("errorMsg : " + errorMsg);
            throw new BizException("999991", errorMsg);
        }
    }

}
