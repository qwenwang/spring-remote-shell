/*
 * Copyright 2018 the original author or authors.
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

package com.qwen.spring.shell.command;

import jline.console.ConsoleReader;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * {@link UserInput} that uses Standard in and out.
 *
 * @author Eric Bottard
 * @author Gary Russell
 */
@Component
public class ConsoleUserInput implements UserInput {

	private ConsoleReader reader;
	@Autowired
	private JLineShellComponent shell;

	public ConsoleReader reader() {
		if(reader == null) {
			try {
				Field field = shell.getClass().getSuperclass().getDeclaredField("reader");
				field.setAccessible(true);
				return (ConsoleReader)field.get(shell);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		return reader;
	}

	/**
	 * Loops until one of the {@code options} is provided. Pressing return is equivalent
	 * to returning {@code defaultValue}.
	 */
	@Override
	public String promptWithOptions(String prompt, String defaultValue, String... options) {
		List<String> optionsAsList = Arrays.asList(options);
		InputStreamReader console = new InputStreamReader(System.in);
		String answer;
		do {
			System.out.format("%s %s: ", prompt, optionsAsList);
			answer = read(console, true);
		}
		while (!optionsAsList.contains(answer) && !"".equals(answer));
		return "".equals(answer) && !optionsAsList.contains("") ? defaultValue : answer;
	}

	@Override
	public String prompt(String prompt, String defaultValue, boolean echo) {
		try {
			boolean historyEnabled = reader().isHistoryEnabled();
			reader().setHistoryEnabled(false);
			String answer = reader().readLine(prompt+":");
			reader().setHistoryEnabled(historyEnabled);
			return "".equals(answer) ? defaultValue : answer;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}

	}

	/**
	 * Reads a single line of input from the console.
	 *
	 * @param console input
	 * @param echo whether the input should be echoed (e.g. false for passwords, other
	 * sensitive data)
	 */
	private String read(InputStreamReader console, boolean echo) {
		StringBuilder builder = new StringBuilder();
		try {
			for (char c = (char) console.read(); !(c == '\n' || c == '\r'); c = (char) console.read()) {
				if (echo) {
					System.out.print(c);
				}
				if(c == '\b') {
					builder.deleteCharAt(builder.length()-1);
				} else {
					builder.append(c);
				}
			}
			System.out.println();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return builder.toString();
	}
}
