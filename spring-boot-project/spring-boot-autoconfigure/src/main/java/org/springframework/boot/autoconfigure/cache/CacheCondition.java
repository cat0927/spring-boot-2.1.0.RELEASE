/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cache;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * General cache condition used with all cache configuration classes.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 1.3.0
 */
class CacheCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		String sourceClass = "";
		if (metadata instanceof ClassMetadata) {
			sourceClass = ((ClassMetadata) metadata).getClassName();
		}
		ConditionMessage.Builder message = ConditionMessage.forCondition("Cache",
				sourceClass);
		Environment environment = context.getEnvironment();
		try {

			// 创建指定环境的 Binder，然后绑定属性到对象上。
			BindResult<CacheType> specified = Binder.get(environment)
					.bind("spring.cache.type", CacheType.class);

			// 如果未绑定，则返回匹配。
			if (!specified.isBound()) {
				return ConditionOutcome.match(message.because("automatic cache type"));
			}

			// 获取所需的缓存类型
			CacheType required = CacheConfigurations
					.getType(((AnnotationMetadata) metadata).getClassName());

			// 如果已绑定，并且绑定的类型与所需的缓存类型相同，则返回匹配
			if (specified.get() == required) {
				return ConditionOutcome
						.match(message.because(specified.get() + " cache type"));
			}
		}
		catch (BindException ex) {
		}

		// 其他情况则返回不匹配。
		return ConditionOutcome.noMatch(message.because("unknown cache type"));
	}

}
