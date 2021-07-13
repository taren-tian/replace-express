package com.pmd.rule.errorprone;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

/**
 * rule 2
 */
public class JavaLangRestrictionRule extends AbstractJavaRule {
    public JavaLangRestrictionRule() {
    }

    @Override
    public Object visit(ASTClassOrInterfaceType node, Object data) {
        String img = node.getImage();

        if (img.contains("Class") || img.contains("Process")
                || img.contains("ClassLoader") || img.contains("System")
                || img.contains("Thread")) {
            this.addViolation(data, node);
        }
        return data;
    }
}
