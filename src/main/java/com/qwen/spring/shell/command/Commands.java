package com.qwen.spring.shell.command;

import com.google.gson.*;
import com.nhsoft.provider.base.dto.Tuple;
import com.nhsoft.provider.core.dto.ResponseDTO;
import com.nhsoft.provider.internal.dto.ResponseCode;
import com.nhsoft.provider.shell.remote.FieldInfo;
import com.nhsoft.provider.shell.remote.MethodInfo;
import com.qwen.spring.shell.config.SpringRemoteShell;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class Commands implements CommandMarker {

    private static final String ECHO = "echo";

    private static final String LS = "ls";

    private static final String CD = "cd";

    private static final String PWD = "pwd";

    private static final String CREATE = "create";

    private static final String CALL = "call";

    private static final String LIST_CONTAINER = "list container";

    private static final String PRINT = "print";

    private static final String CONFIG = "config";

    private static final List<String> fundamentalClasses = Arrays.asList("java.lang.String", "java.math.BigDecimal", "java.lang.Integer",
            "java.lang.Long", "java.lang.Boolean");

    private static final Pattern variablePattern = Pattern.compile("<(\\w+)>");
    @Autowired
    private UserInput userInput;
    @Autowired
    private SpringRemoteShell shell;
    private Gson gson;
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.disableHtmlEscaping();
        gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        gsonBuilder.registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
            try {
                return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(json.getAsString());
            } catch (ParseException var5) {
                return null;
            }
        });
        gsonBuilder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, typeOfT, context) -> {
            try {
                return Integer.parseInt(json.getAsString());
            } catch (Exception var5) {
                return null;
            }
        });
        gsonBuilder.registerTypeAdapter(BigDecimal.class, (JsonDeserializer<BigDecimal>) (json, typeOfT, context) -> {
            try {
                return new BigDecimal(json.getAsString());
            } catch (Exception var5) {
                return null;
            }
        });
        gson = gsonBuilder.create();
    }

    @CliCommand(value = ECHO, help = "检查连接")
    public String echo() {
        return shell.echo();
    }

    @CliCommand(value = LS, help = "列出所有对象")
    public String ls() {
        if(shell.getCurrentComponent() == null) {
            return shell.listComponents().stream().map(c -> String.format("%s[%s]", c.getName(), typeToSimple(c.getType()))).collect(Collectors.joining("\n"));
        } else {
            return shell.listMethods().stream().map(m -> String.format("%s %s(%s)", typeToSimple(m.getReturnType()), m.getName(), m.getParams().stream().map(p -> String.format("%s %s", typeToSimple(p.getType()), p.getName())).collect(Collectors.joining(", ")))).collect(Collectors.joining("\n"));
        }
    }

    @CliCommand(value = PWD, help = "当前位置")
    public String pwd() {
        if(shell.getCurrentComponent() == null) {
            return "/";
        }
        return "/" + shell.getCurrentComponent();
    }

    @CliCommand(value = CD, help = "跳转目录")
    public String use(@CliOption(mandatory = true, key = "", help = "Component或者类名称") String component) {
        shell.useComponent(component);
        return "完成";
    }

    @CliCommand(value = CALL, help = "调用特定Component的Method")
    public String callMethod(@CliOption(mandatory = true, key = "", help = "方法名") String methodName) {
        List<MethodInfo> methods = shell.listMethods().stream().filter(m -> m.getName().equals(methodName)).collect(Collectors.toList());
        if(methods.size() == 0) {
            methods = shell.listMethods().stream().filter(m -> m.getName().contains(methodName)).collect(Collectors.toList());
            if(methods.size() == 0) {
                throw new RuntimeException(String.format("方法[%s]不存在", methodName));
            }
        }
        MethodInfo methodInfo = null;
        if(methods.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<methods.size();i++) {
                MethodInfo method = methods.get(i);
                sb.append(String.format("%d: %s %s(%s)\n", i+1, typeToSimple(method.getReturnType()), method.getName(), method.getParams().stream().map(p -> String.format("%s %s", typeToSimple(p.getType()), p.getName()))));
            }
            sb.append("请选择执行的函数编号");
            String value = userInput.prompt(sb.toString(), "<NULL>", true);
            if("<NULL>".equals(value) || !StringUtils.isNumeric(value)) {
                return "执行取消";
            }
            methodInfo = methods.get(Integer.parseInt(value)-1);
        } else {
            methodInfo = methods.get(0);
        }
        List<Tuple<String, String>> params = new ArrayList<>();
        for(FieldInfo fieldInfo: methodInfo.getParams()) {
            String variable = userInput.prompt(String.format("请输入[%s]的值(类型[%s])", fieldInfo.getName(), fieldInfo.getType()), "<NULL>", true);
            String fieldValue;
            if(variable.equals("<NULL>")) {
                params.add(Tuple.makeTuple(fieldInfo.getType(), variable));
                continue;
            }
            Matcher matcher = variablePattern.matcher(variable);
            if(matcher.find()) {
                Container container = shell.getContainer(matcher.group(1));
                if(!container.getType().equals(fieldInfo.getType())) {
                    throw new RuntimeException(String.format("类型不正确[%s]", fieldInfo.getType()));
                }
                fieldValue = (String)value(container.getValue(), container.getType(), false);
            } else {
                fieldValue = (String)value(variable, fieldInfo.getType(), false);
            }
            params.add(Tuple.makeTuple(fieldInfo.getType(), fieldValue));
        }
        ResponseDTO response = shell.call(methodInfo.getName(), params);
        if(!ResponseCode.SUCCESS.equals(response.getCode())) {
            throw new RuntimeException(String.format("%s:%s", response.getCode(), response.getMsg()));
        }
        String result = (String)response.getResult();
        JsonParser parser = new JsonParser();
        try {
            JsonElement jsonElement = parser.parse(result);
            return gson.toJson(jsonElement);
        } catch (JsonSyntaxException e) {
            return result;
        }
    }

    @CliCommand(value = CREATE, help = "创建对象")
    public String create(@CliOption(mandatory = true, key = "", help = "类名") String className,
                         @CliOption(mandatory = false, key = "name", help = "对象名") String objectName,
                         @CliOption(mandatory = false, key = "value", help = "值")String value) {
        if(fundamentalClasses.contains(className)) {
            return createFundamental(className, objectName, value);
        }
        return createCustom(className, objectName, value);
    }

    @CliCommand(value = LIST_CONTAINER, help = "查看本地缓存对象")
    public String list() {
        return shell.listContainerKeys().stream().collect(Collectors.joining("\n"));
    }

    @CliCommand(value = PRINT, help = "打印对象")
    public String create(@CliOption(mandatory = true, key = "", help = "对象名") String objectName) {
        Container container = shell.getContainer(objectName);
        return String.format("类型:%s\n值:%s", container.getType(), container.getValue());
    }

    private String createFundamental(String className, String objectName, String value) {
        if(value == null) {
            value = userInput.prompt(String.format("请输入[%s]的值", className), "<NULL>", true);
            if(value.equals("<NULL>")) {
                throw new RuntimeException("无效的值");
            }
        }
        return shell.putContainer(objectName, className, (String)value(value, className, false));
    }

    private Object value(String literal, String type, boolean toType) {
        switch (type) {
            case "java.lang.String":
                return literal;
            case "java.math.BigDecimal":
                if(!NumberUtils.isParsable(literal)) {
                    throw new RuntimeException(String.format("值[%s]不是[%s]类型", literal, type));
                }
                if(toType) {
                    return new BigDecimal(literal);
                } else {
                    return literal;
                }
            case "java.lang.Integer":
            case "java.lang.Long":
                if(!StringUtils.isNumeric(literal)) {
                    throw new RuntimeException(String.format("值[%s]不是[%s]类型", literal, type));
                }
                if(toType) {
                    return Long.parseLong(literal);
                } else {
                    return literal;
                }
            default:
                if (toType) {
                    return gson.fromJson(literal, Map.class);
                }
                return literal;
        }
    }

    private String typeToSimple(String type) {
        int index = type.lastIndexOf(".");
        return type.substring(index+1);
    }

    private String createCustom(String className, String objectName, String value) {
        if(value == null) {
            List<FieldInfo> classFields = shell.listClassFields(className);
            Map<String, Object> values = new HashMap<>();
            for(FieldInfo fieldInfo: classFields) {
                String variable = userInput.prompt(String.format("请输入[%s]的值(类型[%s])", fieldInfo.getName(), fieldInfo.getType()), "<NULL>", true);
                Object fieldValue;
                if(variable.equals("<NULL>")) {
                    continue;
                }
                Matcher matcher = variablePattern.matcher(variable);
                if(matcher.find()) {
                    Container container = shell.getContainer(matcher.group(1));
                    if(!container.getType().equals(fieldInfo.getType())) {
                        throw new RuntimeException(String.format("类型不正确[%s]", fieldInfo.getType()));
                    }
                    fieldValue = value(container.getValue(), container.getType(), true);
                } else {
                    fieldValue = value(variable, fieldInfo.getType(), true);
                }
                values.put(fieldInfo.getName(), fieldValue);
            }
            value = gson.toJson(values);
        }
        return shell.putContainer(objectName, className, value);
    }

    @CliCommand(value = CONFIG, help = "config")
    public String configServer(@CliOption(mandatory = true, key = {"", "uri"}, help = "Spring服务的地址")String uri,
                               @CliOption(mandatory = false, key = "prefix", help = "需要扫描的包前缀")String prefix) {
        shell.setUrl(uri);
        if(prefix != null) {
            shell.setPrefix(prefix);
        }
        return "完成";
    }



}
