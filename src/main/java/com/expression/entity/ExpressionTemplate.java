package com.expression.entity;


import com.entity.TestEntity;

/**
 * @author tianliangyu
 */
public interface ExpressionTemplate {

    Object execute(TestEntity testEntity) throws Exception;

}
