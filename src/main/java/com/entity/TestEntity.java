package com.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * data model
 * @author taren.tian
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TestEntity {

    private String var1;
    private String var2;
    private String var3;
    private Integer int1;
    private Integer int2;
    private Map<String, Object> map;
}
