import com.lethanh98.cachefile.CacheFile;
import lombok.*;
import org.junit.jupiter.api.Assertions;

import java.io.Serializable;

public class Test {
    public static final String WORKING_DIR = System.getProperty("user.dir");

    @org.junit.jupiter.api.Test
    public void test() {
        CacheFile<String, TestClass> mapFIle = new CacheFile(WORKING_DIR + "\\src\\test\\testFolder");
        TestClass data = new TestClass("Thành");
        mapFIle.put("t", new TestClass("Thành"));
        Assertions.assertEquals(data, mapFIle.get("t"));
        System.out.println(mapFIle.toList());
        System.out.println(mapFIle.getFileFolder());
        Assertions.assertFalse(mapFIle.isEmpty());
        System.out.println(mapFIle.size() == 1);
        mapFIle.clear();
        Assertions.assertTrue(mapFIle.isEmpty());

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class TestClass implements Serializable {
        private String name;
    }
}
