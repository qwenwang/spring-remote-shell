/*
 * Copyright 2016-2017 the original author or authors.
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

package com.qwen.spring.shell.autoconfigure;

import com.qwen.spring.shell.ShellCommandLineParser;
import com.qwen.spring.shell.ShellProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.JLineShell;
import org.springframework.shell.core.JLineShellComponent;

import javax.annotation.PostConstruct;

/**
 * Configures the various commands that are part of the default Spring Shell experience.
 *
 * @author Josh Long
 * @author Mark Pollack
 * @author Eric Bottard
 */
@Configuration
@ImportResource("classpath*:/META-INF/spring/spring-shell-plugin.xml")
public class BaseShellAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(BaseShellAutoConfiguration.class);

	@Bean
	public ShellCommandLineParser shellCommandLineParser() {
		return new ShellCommandLineParser();
	}

	@Bean
	public ShellProperties shellProperties() {
		return new ShellProperties();
	}

	@Bean
	@ConditionalOnMissingBean(CommandLine.class)
	public CommandLine commandLine(ShellCommandLineParser shellCommandLineParser, ShellProperties shellProperties,
								   ApplicationArguments applicationArguments) throws Exception {
		return shellCommandLineParser.parse(shellProperties, applicationArguments.getSourceArgs());
	}

	@Bean
	@ConditionalOnMissingBean(JLineShell.class)
	public JLineShellComponent shell() {
		return new JLineShellComponent();
	}

	@Configuration
	@ComponentScan({ "org.springframework.shell.commands", "org.springframework.shell.converters", "org.springframework.shell.plugin.support" })
	public static class DefaultShellComponents {

		@PostConstruct
		public void log() {
			logger.debug(
					"default (o.s.shell.{converters,plugin.support})" + " Spring Shell packages are being scanned");
		}
	}

}
