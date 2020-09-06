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

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link Condition} that checks for the presence or absence of
 * {@link WebApplicationContext}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see ConditionalOnWebApplication
 * @see ConditionalOnNotWebApplication
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnWebApplicationCondition extends FilteringSpringBootCondition {

	private static final String SERVLET_WEB_APPLICATION_CLASS = "org.springframework.web.context.support.GenericWebApplicationContext";

	private static final String REACTIVE_WEB_APPLICATION_CLASS = "org.springframework.web.reactive.HandlerResult";

	@Override
	protected ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		for (int i = 0; i < outcomes.length; i++) {
			String autoConfigurationClass = autoConfigurationClasses[i];
			if (autoConfigurationClass != null) {
				outcomes[i] = getOutcome(autoConfigurationMetadata
						.get(autoConfigurationClass, "ConditionalOnWebApplication"));
			}
		}
		return outcomes;
	}

	private ConditionOutcome getOutcome(String type) {
		if (type == null) {
			return null;
		}
		ConditionMessage.Builder message = ConditionMessage
				.forCondition(ConditionalOnWebApplication.class);
		if (ConditionalOnWebApplication.Type.SERVLET.name().equals(type)) {
			if (!ClassNameFilter.isPresent(SERVLET_WEB_APPLICATION_CLASS,
					getBeanClassLoader())) {
				return ConditionOutcome.noMatch(
						message.didNotFind("servlet web application classes").atAll());
			}
		}
		if (ConditionalOnWebApplication.Type.REACTIVE.name().equals(type)) {
			if (!ClassNameFilter.isPresent(REACTIVE_WEB_APPLICATION_CLASS,
					getBeanClassLoader())) {
				return ConditionOutcome.noMatch(
						message.didNotFind("reactive web application classes").atAll());
			}
		}
		if (!ClassNameFilter.isPresent(SERVLET_WEB_APPLICATION_CLASS,
				getBeanClassLoader())
				&& !ClassUtils.isPresent(REACTIVE_WEB_APPLICATION_CLASS,
						getBeanClassLoader())) {
			return ConditionOutcome.noMatch(message
					.didNotFind("reactive or servlet web application classes").atAll());
		}
		return null;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {

		/**
		 * required = true, 说明当前 Configuration Class 添加了 “@ConditionOnWebApplication” 的声明。
		 */
		boolean required = metadata
				.isAnnotated(ConditionalOnWebApplication.class.getName());

		/**
		 * 判断当前 Spring 应用是否是 Web 应用。{@link #isWebApplication(ConditionContext, AnnotatedTypeMetadata, boolean)}
		 */
		ConditionOutcome outcome = isWebApplication(context, metadata, required);
		if (required && !outcome.isMatch()) {

			// 3. 如果有 “@ConditionalOnWebApplication” 注解,但是不是 WebApplication环境,则返回不匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage());
		}
		if (!required && outcome.isMatch()) {

			// 4. 如果没有被 “@ConditionalOnWebApplication” 注解,但是是 WebApplication环境,则返回不匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage());
		}

		// 5. 如果被 “@ConditionalOnWebApplication” 注解,并且是WebApplication环境,则返回不匹配
		return ConditionOutcome.match(outcome.getConditionMessage());
	}

	/**
	 * 判断当前 Spring 应用是否是 Web 应用。
	 *
	 */
	private ConditionOutcome isWebApplication(ConditionContext context,
			AnnotatedTypeMetadata metadata, boolean required) {

		/**
		 * 推断 WEB 类型 {@link #deduceType(AnnotatedTypeMetadata)}
		 */
		switch (deduceType(metadata)) {
		case SERVLET:

			/**
			 *  是否为 `SERVLET` {@link #isServletWebApplication(ConditionContext)}
			 */
			return isServletWebApplication(context);
		case REACTIVE:

			/**
			 * 是否为 `REACTIVE` {@link #isReactiveWebApplication(ConditionContext)}
			 */
			return isReactiveWebApplication(context);
		default:

			// 其他
			return isAnyWebApplication(context, required);
		}
	}

	private ConditionOutcome isAnyWebApplication(ConditionContext context,
			boolean required) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(
				ConditionalOnWebApplication.class, required ? "(required)" : "");
		ConditionOutcome servletOutcome = isServletWebApplication(context);
		if (servletOutcome.isMatch() && required) {
			return new ConditionOutcome(servletOutcome.isMatch(),
					message.because(servletOutcome.getMessage()));
		}
		ConditionOutcome reactiveOutcome = isReactiveWebApplication(context);
		if (reactiveOutcome.isMatch() && required) {
			return new ConditionOutcome(reactiveOutcome.isMatch(),
					message.because(reactiveOutcome.getMessage()));
		}
		return new ConditionOutcome(servletOutcome.isMatch() || reactiveOutcome.isMatch(),
				message.because(servletOutcome.getMessage()).append("and")
						.append(reactiveOutcome.getMessage()));
	}

	private ConditionOutcome isServletWebApplication(ConditionContext context) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("");

		// 是否存在于当前 Class Path 中，如果不存在，则说明当前应用并非 Servlet Web 应用场景
		if (!ClassNameFilter.isPresent(SERVLET_WEB_APPLICATION_CLASS,
				context.getClassLoader())) {
			return ConditionOutcome.noMatch(
					message.didNotFind("servlet web application classes").atAll());
		}

		// 是否存在 “scope” 为session 的 bean。如果存在，说明当前应用属于 Servlet Web 应用。
		if (context.getBeanFactory() != null) {
			String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
			if (ObjectUtils.containsElement(scopes, "session")) {
				return ConditionOutcome.match(message.foundExactly("'session' scope"));
			}
		}

		// 判断 应用上下文所关联的 Environment 是否为 “ConfigurableWebEnvironment”
		if (context.getEnvironment() instanceof ConfigurableWebEnvironment) {
			return ConditionOutcome
					.match(message.foundExactly("ConfigurableWebEnvironment"));
		}

		// 当 Spring 应用上下文属于 “WebApplicationContext”
		if (context.getResourceLoader() instanceof WebApplicationContext) {
			return ConditionOutcome.match(message.foundExactly("WebApplicationContext"));
		}

		// 其他情况,返回不匹配.
		return ConditionOutcome.noMatch(message.because("not a servlet web application"));
	}

	private ConditionOutcome isReactiveWebApplication(ConditionContext context) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("");
		if (!ClassNameFilter.isPresent(REACTIVE_WEB_APPLICATION_CLASS,
				context.getClassLoader())) {
			return ConditionOutcome.noMatch(
					message.didNotFind("reactive web application classes").atAll());
		}
		if (context.getEnvironment() instanceof ConfigurableReactiveWebEnvironment) {
			return ConditionOutcome
					.match(message.foundExactly("ConfigurableReactiveWebEnvironment"));
		}
		if (context.getResourceLoader() instanceof ReactiveWebApplicationContext) {
			return ConditionOutcome
					.match(message.foundExactly("ReactiveWebApplicationContext"));
		}
		return ConditionOutcome
				.noMatch(message.because("not a reactive web application"));
	}

	/**
	 * 推断 应用类型 Type，如果 type() 指定类型，则使用指定类型，否则采用 “Type.ANY”
	 */
	private Type deduceType(AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ConditionalOnWebApplication.class.getName());
		if (attributes != null) {
			return (Type) attributes.get("type");
		}
		return Type.ANY;
	}

}
