package com.lethanh98.cachefile;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Data
@Builder(builderMethodName = "hiddenBuilder")
public class CacheFile<K extends Serializable, V extends Serializable> implements Map<K, V> {
    private File fileFolder;
    private boolean keyEncodeByHashCode;

    public static CacheFileBuilder builder(String urlFolder) {
        File fileFolder = new File(urlFolder);
        if (!fileFolder.isDirectory()) {
            throw new RuntimeException("Url not is folder");
        }
        return hiddenBuilder().fileFolder(fileFolder);
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
        } catch (IOException e) {
            return null;
        }
    }

    public V put(K key, V value) {
        try {
            checkNull(key);
            checkNull(value);
            File persistenceFile = pathToFileFor(key);
            persistenceFile.getParentFile().mkdirs();
            try (FileOutputStream fileOutputStream = new FileOutputStream(persistenceFile)) {
                try (FileLock ignored = fileOutputStream.getChannel().lock()) {
                    persist(value, fileOutputStream);
                    return value;
                }
            }
        } catch (java.io.IOException e) {
            log.error(e.getMessage(), e);
            return null;
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            throw exception;
        }
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
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(value);
        objectOutputStream.flush();
    }

    public List<V> toList() {
        List<V> list = new ArrayList<>();
        for (File file : fileFolder.listFiles()) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                try (FileLock ignored = fileInputStream.getChannel().lock(0, Long.MAX_VALUE, true)) {
                    V v = (V) new ObjectInputStream(fileInputStream).readObject();
                    list.add(v);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return list;
    }

    public Stream<V> toStream() {
        return Arrays.stream(fileFolder.listFiles()).map(file -> {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                FileLock fileLock = fileInputStream.getChannel().lock(0, Long.MAX_VALUE, true);
                try {
                    return readPersisted("key", fileInputStream);
                } finally {
                    fileLock.release();
                }
            } catch (Exception e) {
                log.warn("Error: " + e.getMessage(), e);
                return null;
            }
        }).filter(Objects::nonNull);
    }

    protected V findPersisted(Object key) throws IOException {
        File persistenceFile = pathToFileFor(key);
        if (!persistenceFile.exists()) return null;
        try (FileInputStream fileInputStream = new FileInputStream(persistenceFile)) {
            try (FileLock ignored = fileInputStream.getChannel().lock(0, Long.MAX_VALUE, true)) {
                return readPersisted(key, fileInputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected V readPersisted(Object key, InputStream inputStream) throws IOException {
        try {
            return (V) new ObjectInputStream(inputStream).readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Serialized version assigned by %s was invalid", key), e);
        }
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
}
