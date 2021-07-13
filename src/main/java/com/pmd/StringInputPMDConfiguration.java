package com.pmd;

import net.sourceforge.pmd.PMDConfiguration;
/**
 * pmd configuration
 */
public class StringInputPMDConfiguration extends PMDConfiguration {
    public String getInputContent() {
        return inputContent;
    }

    public String getInputContentName() {
        return inputContentName;
    }

    public void setInputContent(String inputContent) {
        this.inputContent = inputContent;
    }

    public void setInputContentName(String inputContentName) {
        this.inputContentName = inputContentName;
    }

    private String inputContent;
    private String inputContentName;
}
