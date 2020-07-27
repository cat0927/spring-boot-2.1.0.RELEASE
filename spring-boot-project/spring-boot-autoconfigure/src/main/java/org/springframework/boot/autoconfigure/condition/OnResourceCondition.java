/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * {@link Condition} that checks for specific resources.
 *
 * @author Dave Syer
 * @see ConditionalOnResource
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnResourceCondition extends SpringBootCondition {

	private final ResourceLoader defaultResourceLoader = new DefaultResourceLoader();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		/**
		 * 获取注解 元属性信息 “ attributes ”
		 */
		MultiValueMap<String, Object> attributes = metadata
				.getAllAnnotationAttributes(ConditionalOnResource.class.getName(), true);

		// 获取 ResourceLoader 对象 loader。
		ResourceLoader loader = (context.getResourceLoader() != null)
				? context.getResourceLoader() : this.defaultResourceLoader;
		List<String> locations = new ArrayList<>();


		collectValues(locations, attributes.get("resources"));
		Assert.isTrue(!locations.isEmpty(),
				"@ConditionalOnResource annotations must specify at "
						+ "least one resource location");
		List<String> missing = new ArrayList<>();
		for (String location : locations) {
			String resource = context.getEnvironment().resolvePlaceholders(location);

			/**
			 * 解析 @ConditionalOnResource # resources() 属性中可能存在占位符。
			 * 	1、如果均已存在，则说明条件成立。
			 * 	2、否则，条件不成立。
			 */

			if (!loader.getResource(resource).exists()) {
				missing.add(location);
			}
		}
		if (!missing.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage
					.forCondition(ConditionalOnResource.class)
					.didNotFind("resource", "resources").items(Style.QUOTE, missing));
		}
		return ConditionOutcome
				.match(ConditionMessage.forCondition(ConditionalOnResource.class)
						.found("location", "locations").items(locations));
	}

	private void collectValues(List<String> names, List<Object> values) {
		for (Object value : values) {
			for (Object item : (Object[]) value) {
				names.add((String) item);
			}
		}
	}

}
