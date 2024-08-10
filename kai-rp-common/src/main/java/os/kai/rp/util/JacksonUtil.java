package os.kai.rp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonUtil {
    private static final ThreadLocal<ObjectMapper> mapper = ThreadLocal.withInitial(ObjectMapper::new);
    public static <T> String stringify(T obj) throws JsonProcessingException {
        return mapper.get().writeValueAsString(obj);
    }

    public static <T> T parse(String json, Class<T> clas) throws JsonProcessingException {
        return mapper.get().readValue(json,clas);
    }
}
