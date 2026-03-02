package spring.ai.example.springaidemo.manager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PromptManagerTest {

    @Autowired
    private PromptManager promptManager;

    @Test
    void testGetPrompt() {
        // 测试加载存在的 prompt 文件
        String content = promptManager.getPrompt("travel_agent.txt");
        assertNotNull(content, "应该能加载 travel_agent.txt 文件");
        assertFalse(content.isEmpty(), "文件内容不应为空");
        assertTrue(content.contains("知识库"), "文件内容应包含预期文本");

        // 测试缓存机制 - 再次获取应该返回相同内容
        String cachedContent = promptManager.getPrompt("travel_agent.txt");
        assertEquals(content, cachedContent, "缓存的内容应该相同");

        // 测试加载不存在的文件
        String nonExistent = promptManager.getPrompt("non_existent.txt");
        assertNull(nonExistent, "不存在的文件应该返回 null");
    }
}

