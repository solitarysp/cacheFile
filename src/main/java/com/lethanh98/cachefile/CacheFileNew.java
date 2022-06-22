package com.lethanh98.cachefile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Data
@Builder(toBuilder = true, builderClassName = "CacheFileInternalBuilder", builderMethodName = "hiddenBuilder")
public class CacheFileNew<K, V> implements Map<K, V> {
    private ScheduledExecutorService scheduler;
    private File fileFolder;
    private EncodeAndDecode<V> encodeAndDecode;
    private boolean keyEncodeByHashCode;
    private Class<V> classKey;
    private CleanTime cleanTime;

    public static Builder builder(String urlFolder, Class classKey) {
        File fileFolder = new File(urlFolder);
        if (!fileFolder.isDirectory()) {
            fileFolder.mkdirs();
        }
        Builder builder = new Builder();
        builder.fileFolder(fileFolder);
        builder.classKey(classKey);
        return builder;
    }

    public static class Builder extends CacheFileInternalBuilder {
        Builder() {
            super();
        }

        @Override
        public CacheFileNew build() {
            CacheFileNew data = super.build();
            try {
                if (Objects.isNull(data.getEncodeAndDecode())) {
                    data.setEncodeAndDecode(new EncodeAndDecodeDefault());
                }
            } catch (Exception e) {

            }
            if (Objects.nonNull(data.getCleanTime())) {
                if (Objects.isNull(data.getCleanTime().getDuration())) {
                    throw new RuntimeException("duration can not null");
                }
                if (Objects.isNull(data.getCleanTime().getTimeUnit())) {
                    throw new RuntimeException("duration can not null");
                }
                data.setScheduler(Executors.newScheduledThreadPool(1));
                data.toStream().forEach(o -> {
                    Arrays.stream(Objects.requireNonNull(data.getFileFolder().listFiles())).forEach(file -> {
                        data.setTimeOutClean(file.getName(), data.cleanTime);
                    });
                });
            }
            return data;
        }
    }

    @Override
    public int size() {
        return fileFolder.listFiles().length;
    }

    @Override
    public boolean isEmpty() {
        try {
            return fileFolder.listFiles().length <= 0;
        } catch (Exception exception) {
            return true;
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            findPersisted(key);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        try {
            return findPersisted(key);
        } catch (Exception e) {
            return null;
        }
    }

    public V put(K key, V value) {
        try {
            checkNull(key);
            checkNull(value);
            File persistenceFile = pathToFileFor(key);
            try (FileOutputStream fileOutputStream = new FileOutputStream(persistenceFile)) {
                persist(value, fileOutputStream);
                setTimeOutClean(key, cleanTime);
                return value;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw exception;
        }
    }

    private void setTimeOutClean(K key, CleanTime cleanTime) {
        if (Objects.isNull(cleanTime)) {
            return;
        }
        scheduler.schedule(() -> {
            remove(key);
        }, cleanTime.duration, cleanTime.timeUnit);
    }

    @Override
    public V remove(Object key) {
        try {
            File file = pathToFileFor(key);
            V v = get(key);
            if (Objects.nonNull(v)) {
                file.delete();
            }
            return v;
        } catch (Exception exception) {
            throw new RuntimeException("Remove Error");
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        for (File file : fileFolder.listFiles()) {
            file.delete();
        }
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        return toList();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    protected void persist(V value, OutputStream outputStream) throws IOException {
        outputStream.write(encodeAndDecode.encode(value));
    }

    public List<V> toList() {
        List<V> list = new ArrayList<>();
        for (File file : fileFolder.listFiles()) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] bytes = Utils.readAllBytes(fileInputStream);
                V v = encodeAndDecode.decode(classKey, bytes);
                list.add(v);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return list;
    }

    public Stream<V> toStream() {
        return Arrays.stream(Objects.requireNonNull(fileFolder.listFiles())).map(file -> {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                return readPersisted("key", fileInputStream);
            } catch (Exception e) {
                log.warn("Error: " + e.getMessage(), e);
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public List<File> listFileError() {
        return Arrays.stream(Objects.requireNonNull(fileFolder.listFiles())).map(file -> {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] bytes = Utils.readAllBytes(fileInputStream);
                encodeAndDecode.decode(classKey, bytes);
                return null;
            } catch (Exception e) {
                log.warn("Error: " + e.getMessage(), e);
                return file;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean clearFileErrors() {
        List<File> errorFiles = Arrays.stream(Objects.requireNonNull(fileFolder.listFiles())).map(file -> {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                V a = (V) readPersisted("key", fileInputStream);
                return null;
            } catch (Exception e) {
                log.warn("Error: " + e.getMessage(), e);
                return file;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        for (File file : errorFiles) {
            file.delete();
        }
        return true;
    }

    protected V findPersisted(Object key) throws IOException {
        File persistenceFile = pathToFileFor(key);
        if (!persistenceFile.exists()) return null;
        try (FileInputStream fileInputStream = new FileInputStream(persistenceFile)) {
            return readPersisted(key, fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected V readPersisted(Object key, InputStream inputStream) throws IOException {
        byte[] bytes = Utils.readAllBytes(inputStream);
        return encodeAndDecode.decode(classKey, bytes);
    }

    private File pathToFileFor(Object key) {
        File persistenceFile = new File(fileFolder, getKey(key));
        if (persistenceFile.isDirectory()) {
            throw new IllegalArgumentException();
        }
        return persistenceFile;
    }

    private void checkNull(Object data) {
        if (Objects.isNull(data)) {
            throw new NullPointerException();
        }
    }

    private String getKey(Object key) {
        if (isKeyEncodeByHashCode()) {
            return String.valueOf(key.hashCode());
        }
        return key.toString();
    }

    @Data
    @lombok.Builder
    @AllArgsConstructor
    public static class CleanTime {
        private Integer duration;
        private TimeUnit timeUnit;
        private int ThreadPool = 1;
    }
}
