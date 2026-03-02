package spring.ai.example.springaidemo.manager;

import jakarta.annotation.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromptManager {
    private static final Map<String, String> LOADED_PROMPTS = new HashMap<>(10);
    @Resource
    private ResourceLoader resourceLoader;

    public String getPrompt(String filePath) {
        if (!LOADED_PROMPTS.containsKey(filePath)) {
            try {
                org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:prompts/" + filePath);
                if (!resource.exists()) {
                    return null;  // 文件不存在时返回 null
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                    String content = reader.lines().collect(Collectors.joining("\n"));
                    LOADED_PROMPTS.put(filePath, content);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return LOADED_PROMPTS.get(filePath);
    }
}
