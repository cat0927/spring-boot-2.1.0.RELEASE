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

package org.springframework.boot.autoconfigure.cache;

import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.CacheConfigurationImportSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the cache abstraction. Creates a
 * {@link CacheManager} if necessary when caching is enabled via {@link EnableCaching}.
 * <p>
 * Cache store can be auto-detected or specified explicitly via configuration.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see EnableCaching
 *
 *  缓存自动配置
 */
@Configuration

// CacheManager 类是一个缓存管理器的接口。管理各种缓存（cache）组件。
@ConditionalOnClass(CacheManager.class)

// 需要使用 @EnableCaching 时才有效，这是因为该注解隐式的导致了 CacheInterceptor 对应的bean 初始化。
@ConditionalOnBean(CacheAspectSupport.class)
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")

// 加载缓存 CaheProperties 配置项。
@EnableConfigurationProperties(CacheProperties.class)
@AutoConfigureAfter({ CouchbaseAutoConfiguration.class, HazelcastAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class, RedisAutoConfiguration.class })

/**
 * 导入 {@link CacheConfigurationImportSelector} 其实是导入符合条件的 SpringCache 使用的各类基础缓存框架的配置。
 */
@Import(CacheConfigurationImportSelector.class)
public class CacheAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public CacheManagerCustomizers cacheManagerCustomizers(
			ObjectProvider<CacheManagerCustomizer<?>> customizers) {
		return new CacheManagerCustomizers(
				customizers.orderedStream().collect(Collectors.toList()));
	}

	@Bean
	public CacheManagerValidator cacheAutoConfigurationValidator(
			CacheProperties cacheProperties, ObjectProvider<CacheManager> cacheManager) {
		return new CacheManagerValidator(cacheProperties, cacheManager);
	}

	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	protected static class CacheManagerJpaDependencyConfiguration
			extends EntityManagerFactoryDependsOnPostProcessor {

		public CacheManagerJpaDependencyConfiguration() {
			super("cacheManager");
		}

	}

	/**
	 * Bean used to validate that a CacheManager exists and provide a more meaningful
	 * exception.
	 */
	static class CacheManagerValidator implements InitializingBean {

		private final CacheProperties cacheProperties;

		private final ObjectProvider<CacheManager> cacheManager;

		CacheManagerValidator(CacheProperties cacheProperties,
				ObjectProvider<CacheManager> cacheManager) {
			this.cacheProperties = cacheProperties;
			this.cacheManager = cacheManager;
		}

		@Override
		public void afterPropertiesSet() {
			Assert.notNull(this.cacheManager.getIfAvailable(),
					() -> "No cache manager could "
							+ "be auto-configured, check your configuration (caching "
							+ "type is '" + this.cacheProperties.getType() + "')");
		}

	}

	/**
	 * {@link ImportSelector} to add {@link CacheType} configuration classes.
	 */
	static class CacheConfigurationImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {

			/**
			 *  {@link CacheType} 支持的缓存类型
			 */
			CacheType[] types = CacheType.values();
			String[] imports = new String[types.length];
			for (int i = 0; i < types.length; i++) {

				/**
				 *  获得每种缓存对应的自动配置类 {@link CacheConfigurations#getConfigurationClass(CacheType)}
				 */
				imports[i] = CacheConfigurations.getConfigurationClass(types[i]);
			}
			return imports;
		}

	}

}
