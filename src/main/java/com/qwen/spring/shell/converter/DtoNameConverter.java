package com.qwen.spring.shell.converter;

import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;

import java.util.List;

public class DtoNameConverter implements Converter<String> {

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
}
