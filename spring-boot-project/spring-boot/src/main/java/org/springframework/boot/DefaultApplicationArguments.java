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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ApplicationArguments}.
 *
 * @author Phillip Webb
 * @since 1.4.1
 */
public class DefaultApplicationArguments implements ApplicationArguments {

	private final Source source;

	private final String[] args;

	public DefaultApplicationArguments(String[] args) {
		Assert.notNull(args, "Args must not be null");

		/**
		 * source 实现类 {@link Source#Source(String[])}
		 */
		this.source = new Source(args);
		this.args = args;
	}

	@Override
	public String[] getSourceArgs() {
		return this.args;
	}

	@Override
	public Set<String> getOptionNames() {
		String[] names = this.source.getPropertyNames();
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
	}

	@Override
	public boolean containsOption(String name) {
		return this.source.containsProperty(name);
	}

	@Override
	public List<String> getOptionValues(String name) {
		List<String> values = this.source.getOptionValues(name);
		return (values != null) ? Collections.unmodifiableList(values) : null;
	}

	@Override
	public List<String> getNonOptionArgs() {
		return this.source.getNonOptionArgs();
	}

	/**
	 * SimpleCommandLinePropertySource 将命令行参数，分为两组，
	 * 	一为： “选项参数”、
	 * 	二为：“非选项参数”、
	 *
	 * 	例如：--name=张三
	 *
	 * 	解析为 name:张三
	 */
	private static class Source extends SimpleCommandLinePropertySource {

		Source(String[] args) {

			/**
			 *  super {@link SimpleCommandLinePropertySource#SimpleCommandLinePropertySource(String...)}
			 */
			super(args);
		}

		@Override
		public List<String> getNonOptionArgs() {
			return super.getNonOptionArgs();
		}

		@Override
		public List<String> getOptionValues(String name) {
			return super.getOptionValues(name);
		}

	}

}
