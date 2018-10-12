package com.qwen.spring.shell.config;

import com.nhsoft.provider.base.dto.Tuple;
import com.nhsoft.provider.core.dto.ResponseDTO;
import com.nhsoft.provider.shell.remote.FieldInfo;
import com.nhsoft.provider.shell.remote.MethodInfo;
import com.nhsoft.provider.shell.remote.ShellRemoteService;
import com.qwen.spring.shell.command.Container;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpringRemoteShell {

    private ShellRemoteService shellRemoteService;
    private String prefix;
    private String currentComponent;
    private Map<String, Container> containers = new HashMap<>();
    private List<MethodInfo> methods = new ArrayList<>();
    private int tempIndex = 0;

    public void setUrl(String url) {
        HttpInvokerProxyFactoryBean bean = new HttpInvokerProxyFactoryBean();
        bean.setServiceInterface(ShellRemoteService.class);
        bean.setServiceUrl(url+"/shellRemote");
        bean.afterPropertiesSet();
        shellRemoteService = (ShellRemoteService) bean.getObject();
        try {
            echo();
        } catch (Exception e) {
            throw new RuntimeException("连接失败");
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getCurrentComponent() {
        return currentComponent;
    }

    ShellRemoteService get() {
        if(shellRemoteService == null) {
            throw new RuntimeException("服务未初始化");
        }
        return shellRemoteService;
    }

    public String echo() {
        return get().echo();
    }

    public List<FieldInfo> listComponents() {
        return get().listComponents(prefix);
    }

    public void useComponent(String component) {
        currentComponent = component;
        methods = get().listMethods(currentComponent);
    }

    public List<MethodInfo> listMethods() {
        return methods;
    }

    public List<FieldInfo> listClassFields(String className) {
        return get().listClassFields(className);
    }

    public String putContainer(String name, String type, String json) {
        if(name == null) {
            name = "TEMP"+tempIndex++;
        }
        Container container = new Container();
        container.setName(name);
        container.setType(type);
        container.setValue(json);
        containers.put(name, container);
        return name;
    }

    public List<String> listContainerKeys() {
        return new ArrayList<>(containers.keySet());
    }

    public Container getContainer(String name) {
        Container container = containers.get(name);
        if(container == null) {
            throw new RuntimeException(String.format("对象[%s]不存在", name));
        }
        return container;
    }

    public ResponseDTO call(String method, List<Tuple<String, String>> params) {
        return get().callMethod(currentComponent, method, params);
    }
}
