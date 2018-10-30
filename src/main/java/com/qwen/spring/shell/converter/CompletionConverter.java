/*
 * Copyright 2015 the original author or authors.
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

package com.qwen.spring.shell.converter;

import com.nhsoft.provider.shell.remote.FieldInfo;
import com.nhsoft.provider.shell.remote.MethodInfo;
import com.qwen.spring.shell.config.SpringRemoteShell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A converter that provides DSL completion wherever parts of stream definitions may
 * appear.
 *
 * @author Eric Bottard
 */
@Component
public class CompletionConverter implements Converter<String> {

	private static final Pattern NUMBER_OF_INVOCATIONS_CAPTURE = Pattern
			.compile(String.format(".*%s(\\d+).*", TAB_COMPLETION_COUNT_PREFIX));

	/**
	 * To appear in the optionContext. Triggers the use of this converter and specifies
	 * which kind of completion is expected.
	 */
	private static final String COMPLETION_CONTEXT_PREFIX = "completion-";

	@Autowired
	private SpringRemoteShell shell;

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return type == String.class && completionKind(optionContext) != null;
	}

	private String completionKind(String optionContext) {
		String[] options = optionContext.split(" ");
		for (String option : options) {
			if (option.startsWith(COMPLETION_CONTEXT_PREFIX)) {
				return option.substring(COMPLETION_CONTEXT_PREFIX.length());
			}
		}
		return null;
	}

	@Override
	public String convertFromText(String value, Class<?> targetType, String optionContext) {
		return value;
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
			String kind = completionKind(optionContext);
		List<String> proposals = Collections.emptyList();
		switch (kind) {
			case "component":
				proposals = shell.listComponents(existingData, true).stream().map(FieldInfo::getName).collect(Collectors.toList());
				break;
			case "method":
				proposals = shell.listMethods(existingData, true).stream().map(MethodInfo::getName).collect(Collectors.toList());
				break;
			case "common":
				if(shell.getCurrentComponent() == null) {
					proposals = shell.listComponents(existingData, true).stream().map(FieldInfo::getName).collect(Collectors.toList());
				} else {
					proposals = shell.listMethods(existingData, true).stream().map(MethodInfo::getName).collect(Collectors.toList());
				}
				break;
			case "dto":
				shell.listContainerKeys();
				break;
			case "class":
				proposals = shell.listClasses(existingData, false);
				break;

		}
		completions.addAll(proposals.stream().map(Completion::new).collect(Collectors.toList()));
		return false;
	}

	/**
	 * Reads the {@link Converter#TAB_COMPLETION_COUNT_PREFIX} information and determines
	 * how many times the user has pressed the TAB key.
	 */
	private int determineNumberOfInvocations(String optionContext) {
		Matcher matcher = NUMBER_OF_INVOCATIONS_CAPTURE.matcher(optionContext);
		if (matcher.matches()) {
			return Integer.parseInt(matcher.group(1));
		}
		else {
			return 1;
		}
	}

}
