package com.nexlora.nexbusiness.agent;

import com.nexlora.nexbusiness.security.CurrentUser;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    // Spring injects every @Component that implements Tool
    public ToolRegistry(List<Tool> toolList) {
        for (Tool t : toolList) {
            tools.put(t.name(), t);
        }
    }

    private boolean allowed(Tool t) {
        return t.allowedRoles().contains(CurrentUser.agentRole());
    }

    /** Returns the tool only if the current JWT role may use it (blocks hallucinated calls too). */
    public Optional<Tool> get(String name) {
        Tool t = tools.get(name);
        if (t == null || !allowed(t)) return Optional.empty();
        return Optional.of(t);
    }

    // The "tools" array sent to Groq: only tools this role is allowed to use
    public List<Map<String, Object>> definitions() {
        List<Map<String, Object>> defs = new ArrayList<>();
        for (Tool t : tools.values()) {
            if (!allowed(t)) continue;
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", t.name());
            function.put("description", t.description());
            function.put("parameters", t.parametersSchema());

            Map<String, Object> def = new LinkedHashMap<>();
            def.put("type", "function");
            def.put("function", function);
            defs.add(def);
        }
        return defs;
    }
}
