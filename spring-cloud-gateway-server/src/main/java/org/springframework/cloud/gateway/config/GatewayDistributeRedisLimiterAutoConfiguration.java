/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.cloud.gateway.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.distribute.DistributeRedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.web.reactive.DispatcherHandler;


@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(RedisReactiveAutoConfiguration.class)
@AutoConfigureBefore(GatewayAutoConfiguration.class)
@ConditionalOnBean(ReactiveRedisTemplate.class)
@ConditionalOnClass({RedisTemplate.class, DispatcherHandler.class})
@ConditionalOnProperty(name = "spring.cloud.gateway.redis.enabled", matchIfMissing = true)
public class GatewayDistributeRedisLimiterAutoConfiguration {
	@Bean(DistributeRedisRateLimiter.REDIS_SCRIPT_NAME)
	@SuppressWarnings("unchecked")
	public RedisScript redisRequestRateLimiterScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript<>();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("META-INF/scripts/consumer_first_rate_limiter.lua")));
		redisScript.setResultType(List.class);
		return redisScript;
	}

	@Bean
	@Primary
	@ConditionalOnMissingBean
	public DistributeRedisRateLimiter consumeFirstRedisRateLimiter(
			ReactiveStringRedisTemplate redisTemplate,
			@Qualifier(DistributeRedisRateLimiter.REDIS_SCRIPT_NAME) RedisScript<List<Long>> redisScript,
			ConfigurationService configurationService) {
		return new DistributeRedisRateLimiter(redisTemplate, redisScript, configurationService);
	}
}
