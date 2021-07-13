package com.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author taren.tian
 */
public class BizException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(BizException.class);

    private String msg;

    private String code;

    public BizException(String code, String msg) {
        super(msg);
        this.msg = msg;
        this.code = code;
        logger.error(this.msg);
    }
}
