package com.pmd.rule.errorprone;

import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

/**
 * rule 1
 */
public class DoNotAllowImportsRule extends AbstractJavaRule {
    public DoNotAllowImportsRule() {
    }

    @Override
    public Object visit(ASTClassOrInterfaceType node, Object data) {
        String img = node.getImage();
        if (img.startsWith("sun") || img.startsWith("java")) {
            this.addViolation(data, node);
        }
        return data;
    }
}
