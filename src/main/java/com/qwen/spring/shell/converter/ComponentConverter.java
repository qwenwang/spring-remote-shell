package com.qwen.spring.shell.converter;

import com.qwen.spring.shell.config.SpringRemoteShell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class ComponentConverter implements Converter<String> {

    @Autowired
    private SpringRemoteShell springRemoteShell;

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return false;
    }

    @Override
    public String convertFromText(String value, Class<?> targetType, String optionContext) {
        return null;
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
        return false;
    }

    private Optional<String> determineKind(String optionContext) {
        return Arrays.stream(optionContext.split("\\s+"))
                .filter(s -> s.equals("component"))
                .map(s -> s.substring("existing-".length()))
                .findFirst();
    }
}
