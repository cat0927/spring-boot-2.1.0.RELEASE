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

package org.springframework.boot.autoconfigure.jms;

import java.time.Duration;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jms.JmsProperties.DeliveryMode;
import org.springframework.boot.autoconfigure.jms.JmsProperties.Template;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring JMS.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 *
 *  【 JMS 自动装配 】
 */
@Configuration
@ConditionalOnClass({ Message.class, JmsTemplate.class })

// ConnectionFactory 接口用于创建与 JMS 代理进行交互的 javax.jms.Connection 标准方法。
@ConditionalOnBean(ConnectionFactory.class)

// 引入 JMS 配置属性类，对应就是 `spring.jms` 为前缀的属性
@EnableConfigurationProperties(JmsProperties.class)

/**
 * import 引入{@link JmsAnnotationDrivenConfiguration} 用于Spring 4.1 注解驱动的 JMS 自动配置。
 */
@Import(JmsAnnotationDrivenConfiguration.class)
public class JmsAutoConfiguration {

	@Configuration
	protected static class JmsTemplateConfiguration {

		private final JmsProperties properties;

		private final ObjectProvider<DestinationResolver> destinationResolver;

		private final ObjectProvider<MessageConverter> messageConverter;

		public JmsTemplateConfiguration(JmsProperties properties,
				ObjectProvider<DestinationResolver> destinationResolver,
				ObjectProvider<MessageConverter> messageConverter) {
			this.properties = properties;
			this.destinationResolver = destinationResolver;
			this.messageConverter = messageConverter;
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnSingleCandidate(ConnectionFactory.class)
		public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			PropertyMapper map = PropertyMapper.get();

			// 基于 ConnectionFactory 创建 JmsTemplate 对象。
			JmsTemplate template = new JmsTemplate(connectionFactory);

			// 设置是否为 发布订阅模式。
			template.setPubSubDomain(this.properties.isPubSubDomain());
			map.from(this.destinationResolver::getIfUnique).whenNonNull()
					.to(template::setDestinationResolver);
			map.from(this.messageConverter::getIfUnique).whenNonNull()
					.to(template::setMessageConverter);
			mapTemplateProperties(this.properties.getTemplate(), template);
			return template;
		}

		private void mapTemplateProperties(Template properties, JmsTemplate template) {
			PropertyMapper map = PropertyMapper.get();
			map.from(properties::getDefaultDestination).whenNonNull()
					.to(template::setDefaultDestinationName);
			map.from(properties::getDeliveryDelay).whenNonNull().as(Duration::toMillis)
					.to(template::setDeliveryDelay);
			map.from(properties::determineQosEnabled).to(template::setExplicitQosEnabled);
			map.from(properties::getDeliveryMode).whenNonNull().as(DeliveryMode::getValue)
					.to(template::setDeliveryMode);
			map.from(properties::getPriority).whenNonNull().to(template::setPriority);
			map.from(properties::getTimeToLive).whenNonNull().as(Duration::toMillis)
					.to(template::setTimeToLive);
			map.from(properties::getReceiveTimeout).whenNonNull().as(Duration::toMillis)
					.to(template::setReceiveTimeout);
		}

	}

	@Configuration
	@ConditionalOnClass(JmsMessagingTemplate.class)
	@Import(JmsTemplateConfiguration.class)
	protected static class MessagingTemplateConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnSingleCandidate(JmsTemplate.class)
		public JmsMessagingTemplate jmsMessagingTemplate(JmsTemplate jmsTemplate) {
			return new JmsMessagingTemplate(jmsTemplate);
		}

	}

}
