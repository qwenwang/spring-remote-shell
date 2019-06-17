package com.qwen.spring.shell.command;

import com.google.gson.*;
import com.nhsoft.provider.shell.remote.FieldInfo;
import com.nhsoft.provider.shell.remote.GsonWrapper;
import com.nhsoft.provider.shell.remote.MethodInfo;
import com.nhsoft.provider.shell.remote.ResponseDTO;
import com.qwen.spring.shell.config.SpringRemoteShell;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
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

    private static final String SET = "set";

    private static final String CALL = "call";

    private static final String REPEAT = "repeat";

    private static final String LIST = "list";

    private static final String PRINT = "print";

    private static final String WRITE = "write";

    private static final String CONFIG = "config";

    private static final String LOG = "log";

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
                String str = json.getAsString();
                if(str.matches("\\w{4}-\\w{2}-\\w{2}")) {
                    return (new SimpleDateFormat("yyyy-MM-dd")).parse(json.getAsString());
                } else {
                    return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(json.getAsString());
                }
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
    public String ls(@CliOption(mandatory = false, key = {"", "filter"}, help = "过滤") String filter) {
        if(shell.getCurrentComponent() == null) {
            return shell.listComponents(filter, false).stream().map(c -> String.format("%s[%s]", c.getName(), typeToSimple(c.getType()))).collect(Collectors.joining("\n"));
        } else {
            return shell.listMethods(filter, false).stream().map(m -> String.format("%s %s(%s)", typeToSimple(m.getReturnType()), m.getName(), m.getParams().stream().map(p -> String.format("%s %s", typeToSimple(p.getType()), p.getName())).collect(Collectors.joining(", ")))).collect(Collectors.joining("\n"));
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
    public String use(@CliOption(mandatory = true, key = {"", "component"}, optionContext = "completion-component disable-string-converter", help = "Component或者类名称") String component) {
        if("..".equals(component)) {
            shell.useComponent(null);
        } else {
            shell.useComponent(component);
        }
        return "完成";
    }

    @CliCommand(value = CALL, help = "调用特定Component的Method")
    public String callMethod(@CliOption(mandatory = true, key = {"", "method"}, optionContext = "completion-method disable-string-converter", help = "方法名") String methodName) throws IOException {
        List<MethodInfo> methods = shell.listMethods(null, false).stream().filter(m -> m.getName().equals(methodName)).collect(Collectors.toList());
        if(methods.size() == 0) {
            methods = shell.listMethods(methodName, false);
            if(methods.size() == 0) {
                throw new RuntimeException(String.format("方法[%s]不存在", methodName));
            }
        }
        MethodInfo methodInfo;
        if(methods.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<methods.size();i++) {
                MethodInfo method = methods.get(i);
                sb.append(String.format("%d: %s %s(%s)\n", i+1, typeToSimple(method.getReturnType()), method.getName(), method.getParams().stream().map(p -> String.format("%s %s", typeToSimple(p.getType()), p.getName())).collect(Collectors.joining(", "))));
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
        List<Pair<String, String>> params = new ArrayList<>();
        for(FieldInfo fieldInfo: methodInfo.getParams()) {
            String variable = userInput.prompt(String.format("请输入[%s]的值(类型[%s])", fieldInfo.getName(), fieldInfo.getType()), "<NULL>", true);
            String fieldValue;
            if(variable.equals("<SKIP>")) {
                return null;
            }
            if(variable.equals("<NULL>")) {
                params.add(Pair.of(fieldInfo.getType(), variable));
                continue;
            }
            if(variable.equals("<CREATE>")) {
                System.out.format("开始创建[%s]\n", fieldInfo.getName());
                create(fieldInfo.getType(), "PARAM", null, null);
                fieldValue = (String)value(shell.getContainer("PARAM").getValue(), fieldInfo.getType(), false);
            }
            else if(variable.equals("<SIMPLE>")) {
                System.out.format("开始创建[%s]\n", fieldInfo.getName());
                create(fieldInfo.getType(), "PARAM", true, null);
                fieldValue = (String)value(shell.getContainer("PARAM").getValue(), fieldInfo.getType(), false);
            }
            else {
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
            }
            params.add(Pair.of(fieldInfo.getType(), fieldValue));
        }
        ResponseDTO response = shell.call(methodInfo, params);
        if(response.getCode() != 0) {
            throw new RuntimeException(String.format("%s:%s", response.getCode(), response.getMsg()));
        }
        String result = (String)response.getResult();
        shell.putContainer("RESULT", methodInfo.getReturnType(), result);
        JsonParser parser = new JsonParser();
        try {
            JsonElement jsonElement = parser.parse(result);
            return gson.toJson(jsonElement);
        } catch (JsonSyntaxException e) {
            return result;
        }
    }

    @CliCommand(value = REPEAT, help = "重复调用之前的方法")
    public String repeat() {
        ResponseDTO response = shell.repeat();
        if(response.getCode() != 0) {
            throw new RuntimeException(String.format("%s:%s", response.getCode(), response.getMsg()));
        }
        String result = (String)response.getResult();
        shell.putContainer("RESULT", shell.getHistory().getMiddle().getReturnType(), result);
        JsonParser parser = new JsonParser();
        try {
            JsonElement jsonElement = parser.parse(result);
            return gson.toJson(jsonElement);
        } catch (JsonSyntaxException e) {
            return result;
        }
    }

    @CliCommand(value = CREATE, help = "创建对象")
    public String create(@CliOption(mandatory = true, key = {"", "type"}, optionContext = "completion-class disable-string-converter", help = "类名") String className,
                         @CliOption(key = "name", help = "对象名") String objectName,
                         @CliOption(key = "simple", help = "简单模式") Boolean simple,
                         @CliOption(key = "input", help = "文件路径") String path) throws IOException {
        if(path != null) {
            return shell.putContainer(objectName, className, FileUtils.readFileToString(new File(path)));
        }
        if(fundamentalClasses.contains(className)) {
            return createFundamental(className, objectName);
        }
        return createCustom(className, objectName, simple);
    }

    @CliCommand(value = SET, help = "修改对象属性")
    public String set(@CliOption(mandatory = true, key = {"", "name"}, optionContext = "completion-attribute disable-string-converter", help = "") String attributeName,
                         @CliOption(key = "value", help = "值") String value) throws IOException {
        return "hello world";
    }

    @CliCommand(value = LIST, help = "查看本地缓存对象")
    public String list() {
        return shell.listContainerKeys().stream().collect(Collectors.joining("\n"));
    }

    @CliCommand(value = LOG, help = "打印日志")
    public String log() {
        return shell.log();
    }

    @CliCommand(value = PRINT, help = "打印对象")
    public String print(@CliOption(mandatory = true, key = {"", "name"}, optionContext = "completion-dto disable-string-converter", help = "对象名") String objectName) {
        Container container = shell.getContainer(objectName);
        return String.format("类型:%s\n值:%s", container.getType(), container.getValue());
    }

    @CliCommand(value = WRITE, help = "将对象写入文件")
    public String write(@CliOption(mandatory = true, key = {"", "name"}, optionContext = "completion-dto disable-string-converter", help = "对象名") String objectName,
                        @CliOption(key = "output", help = "文件路径") String path) throws IOException {
        if(path == null) {
            path = userInput.prompt("请输入文件路径", "<NULL>", true);
            if("<NULL>".equals(path)) {
                throw new RuntimeException("无效的值");
            }
        }
        Container container = shell.getContainer(objectName);
        FileUtils.writeStringToFile(new File(path), container.getValue());
        return "完成";
    }

    private String createFundamental(String className, String objectName) {
        String value = userInput.prompt(String.format("请输入[%s]的值", className), "<NULL>", true);
        if("<NULL>".equals(value)) {
            throw new RuntimeException("无效的值");
        }
        return shell.putContainer(objectName, className, (String)value(value, className, false));
    }

    private Object value(String literal, String type, boolean toType) {
        switch (type) {
            case "java.lang.String":
            case "java.util.Date":
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
                    if(literal.startsWith("{")) {
                        return gson.fromJson(literal, Map.class);
                    }
                    else if(literal.startsWith("[")) {
                        Type t = List.class;
                        if(!literal.contains("\"") && !literal.contains(".")) {
                            t = TypeUtils.parameterize(List.class, Integer.class);
                        }
                        return gson.fromJson(literal, t);
                    }
                }
                return literal;
        }
    }

    private String typeToSimple(String type) {
        int index = type.lastIndexOf(".");
        return type.substring(index+1);
    }

    private String createCustom(String className, String objectName, Boolean simple) {
        String value;
        if(simple != null && simple) {
            value = userInput.prompt(String.format("请输入[%s]的值", className), "<NULL>", true);
            if("<NULL>".equals(value)) {
                throw new RuntimeException("无效的值");
            }
        }
        else {
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
    public String configServer(@CliOption(key = {"", "uri"}, help = "Spring服务的地址")String uri,
                               @CliOption(key = "prefix", help = "需要扫描的包前缀")String prefix,
                               @CliOption(key = "logLevel", help = "日志等级")String logLevel,
                               @CliOption(key = "pass", help = "密码")String pass,
                               @CliOption(key = "enableDatabaseLog", help = "启用数据库日志")Boolean enableDatabaseLog) {
        if(pass != null) {
            shell.setPass(pass);
        }
        if(uri != null) {
            shell.setUrl(uri);
        }
        if(prefix != null) {
            shell.setPrefix(prefix);
        }
        if(logLevel != null) {
            shell.setLogLevel(logLevel);
        }
        if(enableDatabaseLog != null) {
            shell.setEnableDatabaseLog(enableDatabaseLog);
        }
        return "完成";
    }



}
