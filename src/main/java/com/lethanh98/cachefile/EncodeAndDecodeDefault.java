package com.lethanh98.cachefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

public class EncodeAndDecodeDefault<T> implements EncodeAndDecode<T> {

  private ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public byte[] encode(T data) throws RuntimeException {
    try {
      return objectMapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T decode(Class<T> clazz, byte[] data) throws RuntimeException {
    try {
      return objectMapper.readValue(data, clazz);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
