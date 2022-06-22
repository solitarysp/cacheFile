import com.lethanh98.cachefile.CacheFileNew;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

public class Test {

  public static final String WORKING_DIR = System.getProperty("user.dir");

  @org.junit.jupiter.api.Test
  public void test() {
    CacheFileNew<TestClass, TestClass> mapFIle = CacheFileNew.builder(
        WORKING_DIR + "\\src\\test\\testFolder",
        TestClass.class).build();
    TestClass data = new TestClass("Thành");
    mapFIle.put(data, new TestClass("Thành"));
    TestClass data2 = mapFIle.get(data);
      System.out.println("");

  }

  @org.junit.jupiter.api.Test
  public void CleanTime_1() throws InterruptedException {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    CacheFileNew.CleanTime cleanTime = CacheFileNew.CleanTime.builder()
        .duration(10)
        .timeUnit(TimeUnit.SECONDS)
        .build();
    CacheFileNew<String, TestClass> mapFIle = CacheFileNew.builder(
            WORKING_DIR + "\\src\\test\\CleanTime_1",
            TestClass.class)
        .cleanTime(cleanTime)
        .build();
    CountDownLatch countDownLatch = new CountDownLatch(1_000);
    for (int i = 0; i < 1_000; i++) {
      TestClass data = new TestClass("Thành " + i);
      mapFIle.put("Thành " + i, data);
    }
    scheduler.schedule(() -> {
      countDownLatch.countDown();
    }, cleanTime.getDuration() + 5, cleanTime.getTimeUnit());
    countDownLatch.await();
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
