package com.imooc.miaoshaproject.config;

import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;

/***********************************************************                                          *
 * Time: 2021/8/25
 * Author: HuHong
 * Desc: redis config
 ***********************************************************/

@Component
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {
}
