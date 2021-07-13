package com.response;

import java.time.LocalDateTime;
import java.util.HashMap;


/**
 * a  easy response
 * @author taren.tian
 */
public class R extends HashMap<String, Object> {

    public R() {
        super.put("code", "200");
        super.put("tranDate", LocalDateTime.now());
        super.put("msg", "success");
    }

    public static R SUCCESS() {
        return new R();
    }

    @Override
    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
