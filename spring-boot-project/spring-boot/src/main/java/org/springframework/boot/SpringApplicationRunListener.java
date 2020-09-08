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

package org.springframework.boot;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Listener for the {@link SpringApplication} {@code run} method.
 * {@link SpringApplicationRunListener}s are loaded via the {@link SpringFactoriesLoader}
 * and should declare a public constructor that accepts a {@link SpringApplication}
 * instance and a {@code String[]} of arguments. A new
 * {@link SpringApplicationRunListener} instance will be created for each run.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public interface SpringApplicationRunListener {

	/**
	 * Called immediately when the run method has first started. Can be used for very
	 * early initialization.
	 *
	 * Spring 应用刚启动
	 *  当Run 方法第一次被执行时，会被立即调用，可用于非常早期的初始化工作。
	 *
	 */
	void starting();

	/**
	 * Called once the environment has been prepared, but before the
	 * {@link ApplicationContext} has been created.
	 * @param environment the environment
	 *
	 *                    【 ConfigurableEnvironment 准备妥当，允许将其调整。 】
	 *
	 *                    【 refresh() 调用之前 】
	 *    当 environment 准备完成，在ApplicationContext 创建之前，该方法被调用。
	 */
	void environmentPrepared(ConfigurableEnvironment environment);

	/**
	 * Called once the {@link ApplicationContext} has been created and prepared, but
	 * before sources have been loaded.
	 * @param context the application context
	 *
	 *
	 *                【 ConfigurableApplicationContext 准备妥当，运行将其调整 】
	 *                1、该方法是在 `AbstractApplicationContext # refresh()` 方法之前执行。
	 *
	 *
	 *                AbstractApplicationContext 是 ConfigurableApplicationContext 接口的实现类。
	 *			当ApplicationContext 构建完成，资源还未被加载时，该方法被调用。
	 */
	void contextPrepared(ConfigurableApplicationContext context);

	/**
	 * Called once the application context has been loaded but before it has been
	 * refreshed.
	 * @param context the application context
	 *
	 *                【 ConfigurableApplicationContext 已装载，但为启动 】
	 *
	 *                【 refresh() 调用之前 】
	 *    ApplicationContext 加载完成，未被刷新之前，被调用
	 */
	void contextLoaded(ConfigurableApplicationContext context);

	/**
	 * The context has been refreshed and the application has started but
	 * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
	 * ApplicationRunners} have not been called.
	 * @param context the application context.
	 * @since 2.0.0
	 *
	 * 				【 ConfigurableApplicationContext 已启动，此时 Spring Bean 已初始化完成 】
	 *
	 * 			    【 refresh() 调用之前 】
	 * 	ApplicationContext 刷新并启动之后，CommandLineRunner 和 ApplicationRunner 未被调用之前，该方法被调用。
	 */
	void started(ConfigurableApplicationContext context);

	/**
	 * Called immediately before the run method finishes, when the application context has
	 * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
	 * {@link ApplicationRunner ApplicationRunners} have been called.
	 * @param context the application context.
	 * @since 2.0.0
	 *
	 * 				【 Spring 正在运行 】
	 *
	 * 		       【 refresh() 调用之后 】
	 * 	准备工作就绪，run 方法执行完成之前，该方法被调用
	 */
	void running(ConfigurableApplicationContext context);

	/**
	 * Called when a failure occurs when running the application.
	 * @param context the application context or {@code null} if a failure occurred before
	 * the context was created
	 * @param exception the failure
	 * @since 2.0.0
	 *
	 * 			【 Spring 运行失败 】
	 *
	 * 			【 refresh() 调用之后 】
	 * 	当应用程序出现错误时，该方法被调用。
	 */
	void failed(ConfigurableApplicationContext context, Throwable exception);

}
