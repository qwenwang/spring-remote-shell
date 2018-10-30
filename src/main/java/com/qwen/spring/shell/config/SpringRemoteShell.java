package com.qwen.spring.shell.config;

import com.nhsoft.provider.base.dto.Tuple;
import com.nhsoft.provider.core.dto.ResponseDTO;
import com.nhsoft.provider.shell.remote.FieldInfo;
import com.nhsoft.provider.shell.remote.MethodInfo;
import com.nhsoft.provider.shell.remote.ShellRemoteService;
import com.qwen.spring.shell.command.Container;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SpringRemoteShell {

    private ShellRemoteService shellRemoteService;
    private String prefix;
    private String currentComponent;
    private Map<String, Container> containers = new HashMap<>();
    private List<FieldInfo> components = Collections.emptyList();
    private List<MethodInfo> methods = Collections.emptyList();
    private int tempIndex = 0;

    public void setUrl(String url) {
        HttpInvokerProxyFactoryBean bean = new HttpInvokerProxyFactoryBean();
        bean.setServiceInterface(ShellRemoteService.class);
        bean.setServiceUrl(url+"/shellRemote");
        bean.afterPropertiesSet();
        shellRemoteService = (ShellRemoteService) bean.getObject();
        try {
            echo();
            refresh();
        } catch (Exception e) {
            throw new RuntimeException("连接失败");
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        if(shellRemoteService != null) {
            refresh();
        }
    }

    private void refresh() {
        components = get().listComponents(prefix);
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

    public List<FieldInfo> listComponents(String filter, boolean prefix) {
        if(filter == null) {
            return components;
        }
        if(prefix) {
            return components.stream().filter(c -> c.getName().startsWith(filter)).collect(Collectors.toList());
        } else {
            return components.stream().filter(c -> c.getName().contains(filter)).collect(Collectors.toList());
        }
    }

    public void useComponent(String component) {
        currentComponent = component;
        if(currentComponent != null) {
            components.stream().filter(c -> c.getName().equals(component)).findAny().orElseThrow(() -> new RuntimeException(String.format("component[%s]不存在", component)));
            methods = get().listMethods(currentComponent);
        } else {
            methods = Collections.emptyList();
        }
    }

    public List<MethodInfo> listMethods(String filter, boolean prefix) {
        if(filter == null) {
            return methods;
        }
        if(prefix) {
            return methods.stream().filter(m -> m.getName().startsWith(filter)).collect(Collectors.toList());
        } else {
            return methods.stream().filter(m -> m.getName().contains(filter)).collect(Collectors.toList());
        }
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
        return String.format("<%s>", name);
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
