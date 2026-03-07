package spring.ai.example.springaidemo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import spring.ai.example.springaidemo.manager.PromptManager;
import spring.ai.example.springaidemo.tools.DateTimeTools;
import spring.ai.example.springaidemo.util.GsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TravelAgentService {
    @Autowired
    private PromptManager promptManager;
    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private DeepSeekChatModel chatModel;
    @Autowired
    private DateTimeTools dateTimeTools;
    @Autowired
    private ToolCallbackProvider toolCallbackProvider;
    @Autowired
    private VectorStore vectorStore;

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

    public Flux<String> chat(String query, String conversationId){
        List<Message> messages = new ArrayList<>();
        //添加系统提示词
        messages.add(new SystemMessage(promptManager.getPrompt("system.txt")
                //.replace("{currentDate}", DateUtils.getCurrentDate())
                .replace("{languageType}", "中文")));
        //添加历史消息
        messages.addAll(chatMemory.get(conversationId));
        //添加用户提示词
        messages.add(new UserMessage(promptManager.getPrompt("travel_agent.txt")
                .replace("{expertKnowledge}", "")
                .replace("{hotelKnowledge}", "")
                .replace("{poiKnowledge}", "")
                .replace("{longTailKnowledge}", "")
                .replace("{userQuery}", query)));

        //调用llm
        StringBuilder responseMessage = new StringBuilder();
        return chatModel.stream(new Prompt(messages))
                .map(chatResponse -> chatResponse.getResult().getOutput().getText())
                .doOnNext(responseMessage::append)
                .doOnComplete(() -> {
                    //保存历史消息
                    chatMemory.add(conversationId, new UserMessage(query));
                    chatMemory.add(conversationId, new AssistantMessage(responseMessage.toString()));
                });
    }

    public Flux<String> chatV2(String query, String conversationId){

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // chat-memory advisor
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.5d).topK(6).filterExpression("type == 'mdd'")
                                        .build())
                                .build()    // RAG advisor
                )
                .build();

        return chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .system(s -> s
                        .text(promptManager.getPrompt("system.txt"))
                        .param("languageType", "中文"))
                .user(query)
                .tools(dateTimeTools)
                .toolCallbacks(toolCallbackProvider)
                //.templateRenderer(StTemplateRenderer.builder().startDelimiterToken('{').endDelimiterToken('}').build())
                .stream()
                .content();
    }
}
