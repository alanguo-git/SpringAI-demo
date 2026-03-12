package spring.ai.example.springaidemo.controller;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import spring.ai.example.springaidemo.service.TravelAgentService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/travel")
public class TravelAgentController {
    @Autowired
    private TravelAgentService travelAgentService;

    @GetMapping(value = "/chat", produces = "text/stream;charset=UTF-8")
    public Flux<String> chat(@RequestParam(value = "query") String query,
                                         @RequestParam String conversationId){
        return travelAgentService.chatV2(query, conversationId);
    }

    @GetMapping(value = "/history", produces = "application/json")
    public List<Message> history(@RequestParam String conversationId){
        return travelAgentService.history(conversationId);
    }
}
