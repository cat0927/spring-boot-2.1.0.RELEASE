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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * Bean to handle {@link DataSource} initialization by running {@literal schema-*.sql} on
 * {@link InitializingBean#afterPropertiesSet()} and {@literal data-*.sql} SQL scripts on
 * a {@link DataSourceSchemaCreatedEvent}.
 *
 * @author Stephane Nicoll
 * @see DataSourceAutoConfiguration
 */
class DataSourceInitializerInvoker
		implements ApplicationListener<DataSourceSchemaCreatedEvent>, InitializingBean {

	private static final Log logger = LogFactory
			.getLog(DataSourceInitializerInvoker.class);

	private final ObjectProvider<DataSource> dataSource;

	private final DataSourceProperties properties;

	private final ApplicationContext applicationContext;

	private DataSourceInitializer dataSourceInitializer;

	private boolean initialized;

	DataSourceInitializerInvoker(ObjectProvider<DataSource> dataSource,
			DataSourceProperties properties, ApplicationContext applicationContext) {
		this.dataSource = dataSource;
		this.properties = properties;
		this.applicationContext = applicationContext;
	}

	/**
	 *
	 * 实现 InitializingBean 接口。当 beanFactory 设置完属性之后，会调用 `afterPropertiesSet` 完成自定义操作
	 */
	@Override
	public void afterPropertiesSet() {

		/**
		 * 获取 DataSourceInitializer 基于 DataSourceProperties 初始化 DataSource.{@link #getDataSourceInitializer()}
		 */
		DataSourceInitializer initializer = getDataSourceInitializer();
		if (initializer != null) {

			/**
			 * 执行 DDL 语句（schema-*.sql）{@link DataSourceInitializer#createSchema()}
			 */
			boolean schemaCreated = this.dataSourceInitializer.createSchema();
			if (schemaCreated) {

				/**
				 * 初始化 {@link #initialize(DataSourceInitializer)}
				 */
				initialize(initializer);
			}
		}
	}

	private void initialize(DataSourceInitializer initializer) {
		try {

			// 发布 `DataSourceSchemaCreatedEvent` 事件
			this.applicationContext.publishEvent(
					new DataSourceSchemaCreatedEvent(initializer.getDataSource()));
			// The listener might not be registered yet, so don't rely on it.

			// 此时，监听器可能尚未注册，不能完全依赖，因此主动调用。
			if (!this.initialized) {

				/**
				 * {@link DataSourceInitializer#initSchema()}
				 */
				this.dataSourceInitializer.initSchema();
				this.initialized = true;
			}
		}
		catch (IllegalStateException ex) {
			logger.warn("Could not send event to complete DataSource initialization ("
					+ ex.getMessage() + ")");
		}
	}

	/**
	 * 实现 ApplicationListener 接口，具有事件监听。
	 * @param event
	 */
	@Override
	public void onApplicationEvent(DataSourceSchemaCreatedEvent event) {
		// NOTE the event can happen more than once and
		// the event datasource is not used here

		/**
		 * 事件发生在 {@link #initialize(DataSourceInitializer)}
		 *
		 *  事件可能发生多次，这里未使用数据源事件。
		 *
		 */
		DataSourceInitializer initializer = getDataSourceInitializer();
		if (!this.initialized && initializer != null) {
			initializer.initSchema();
			this.initialized = true;
		}
	}

	private DataSourceInitializer getDataSourceInitializer() {
		if (this.dataSourceInitializer == null) {
			DataSource ds = this.dataSource.getIfUnique();
			if (ds != null) {
				this.dataSourceInitializer = new DataSourceInitializer(ds,
						this.properties, this.applicationContext);
			}
		}
		return this.dataSourceInitializer;
	}

}
