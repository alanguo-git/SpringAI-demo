package spring.ai.example.springaidemo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 对标注了 @Tool 的方法进行环绕切面，统一打印工具被调用的日志。
 */
@Aspect
@Component
public class ToolLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ToolLoggingAspect.class);

    @Around("@annotation(tool)")
    public Object logToolCall(ProceedingJoinPoint joinPoint, Tool tool) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String toolName = className + "#" + methodName;
        String description = tool.description().isEmpty() ? "" : " (" + tool.description() + ")";

        Object[] args = joinPoint.getArgs();
        String argsStr = args == null || args.length == 0 ? "" : ", 参数: " + Arrays.toString(args);

        log.info("[工具调用] 工具: {}{}{}", toolName, description, argsStr);

        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[工具调用] 工具: {} 执行完成, 耗时: {} ms", toolName, cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[工具调用] 工具: {} 执行异常, 耗时: {} ms, 异常: {}", toolName, cost, e.getMessage());
            throw e;
        }
    }
}
