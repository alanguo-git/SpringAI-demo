package spring.ai.example.springaidemo.controller;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import spring.ai.example.springaidemo.tools.DateTimeTools;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class DemoAgentController {
    @Autowired
    private DeepSeekChatModel chatModel;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private DateTimeTools dateTimeTools;
    @Autowired
    private ToolCallbackProvider toolCallbackProvider; //会自动加载mcp client获取到的工具
    @Autowired
    private VectorStore vectorStore; //默认使用SimpleVectorStore

    @PostConstruct
    public void initializeVectorStore() {
        // 初始化向量数据库数据
        loadDataToVectorStore();
    }

    private void loadDataToVectorStore() {
        try{
            // 创建文档并添加到向量存储
            Document doc1 = new Document("古北水镇，门票价格80元，美团优惠价格50元，开放时间为09:00-22:30", Map.of("type", "mdd"));
            //Document doc2 = new Document("酒店预订信息");
            vectorStore.add(List.of(doc1));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //有记忆对话, 按userId保存记录
    @GetMapping(value = "/memoryChatUnique", produces = "text/stream;charset=UTF-8")
    public Flux<String> memoryChatUnique(@RequestParam(value = "message", defaultValue = "你是谁") String message,
                                         @RequestParam String conversationId){
        //写法1：手动添加history
        //UserMessage userMessage = new UserMessage(message);
        //chatMemory.add(conversationId, userMessage);
        //StringBuilder responseMessage = new StringBuilder();
        //return chatModel.stream(new Prompt(chatMemory.get(conversationId)))
        //        .map(chatResponse -> chatResponse.getResult().getOutput().getText())
        //        .doOnNext(responseMessage::append)
        //        .doOnComplete(() -> chatMemory.add(conversationId, new AssistantMessage(responseMessage.toString())));

        //写法2：使用advisor
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build() // chat-memory advisor// RAG advisor
                )
                .build();
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    //工具调用
    @GetMapping(value = "/toolCall", produces = "text/stream;charset=UTF-8")
    public Flux<String> toolCall(@RequestParam(value = "message") String message,
                                         @RequestParam String conversationId){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build() // chat-memory advisor// RAG advisor
                )
                .build();
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        return chatClient.prompt()
                .tools(dateTimeTools)
                .toolCallbacks(toolCallbackProvider)
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    //rag
    @GetMapping(value = "/rag", produces = "text/stream;charset=UTF-8")
    public Flux<String> rag(@RequestParam(value = "message") String message,
                                 @RequestParam String conversationId){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .similarityThreshold(0.5d).topK(6)
                                .build())
                        .build())
                .build();
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "type == 'mdd'"))
                .stream()
                .content();
    }
}
