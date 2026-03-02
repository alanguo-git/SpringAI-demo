package spring.ai.example.springaidemo.util;

import com.google.gson.Gson;

public class GsonUtils {
    public static String toJsonString(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }
}
