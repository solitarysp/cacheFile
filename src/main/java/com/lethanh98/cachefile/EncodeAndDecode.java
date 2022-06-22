package com.lethanh98.cachefile;

public interface EncodeAndDecode<T> {

  byte[] encode(T data) throws RuntimeException;

  T decode(Class<T> clazz, byte[] data) throws RuntimeException;
}
