package com.nexlora.nexbusiness.agent;

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

    public Optional<Tool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    // The "tools" array sent to Groq (OpenAI format)
    public List<Map<String, Object>> definitions() {
        List<Map<String, Object>> defs = new ArrayList<>();
        for (Tool t : tools.values()) {
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
