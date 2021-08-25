package com.imooc.miaoshaproject.service;

/***********************************************************                                          *
 * Time: 2021/8/25
 * Author: HuHong
 * Desc: 封装本地缓存操作类
 ***********************************************************/

public interface CacheService {
    void setCommonCache(String key, Object value);
    Object getFromCommonCache(String key);
}
