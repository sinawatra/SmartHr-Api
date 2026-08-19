package com.smarthr.smarthr.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private RedisService redisService;

    @Test
    void set_Value() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.set("key", "value");

        verify(valueOperations).set("key", "value");
    }

    @Test
    void set_ValueWithTimeout() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisService.set("key", "value", 10, TimeUnit.MINUTES);

        verify(valueOperations).set("key", "value", 10, TimeUnit.MINUTES);
    }

    @Test
    void get_ReturnsValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn("value");

        Object result = redisService.get("key");

        assertEquals("value", result);
    }

    @Test
    void get_Typed_ReturnsCastedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn("value");

        String result = redisService.get("key", String.class);

        assertEquals("value", result);
    }

    @Test
    void get_Typed_NullValue_ReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn(null);

        String result = redisService.get("key", String.class);

        assertNull(result);
    }

    @Test
    void delete_SingleKey() {
        when(redisTemplate.delete("key")).thenReturn(true);

        Boolean result = redisService.delete("key");

        assertTrue(result);
        verify(redisTemplate).delete("key");
    }

    @Test
    void delete_CollectionKeys() {
        Collection<String> keys = List.of("key1", "key2");
        when(redisTemplate.delete(keys)).thenReturn(2L);

        Long result = redisService.delete(keys);

        assertEquals(2L, result);
        verify(redisTemplate).delete(keys);
    }

    @Test
    void hasKey_ReturnsTrue() {
        when(redisTemplate.hasKey("key")).thenReturn(true);

        Boolean result = redisService.hasKey("key");

        assertTrue(result);
    }

    @Test
    void expire_SetsExpiration() {
        when(redisTemplate.expire("key", 60, TimeUnit.SECONDS)).thenReturn(true);

        Boolean result = redisService.expire("key", 60, TimeUnit.SECONDS);

        assertTrue(result);
    }

    @Test
    void getExpire_ReturnsLong() {
        when(redisTemplate.getExpire("key")).thenReturn(300L);

        Long result = redisService.getExpire("key");

        assertEquals(300L, result);
    }

    @Test
    void getExpire_WithTimeUnit() {
        when(redisTemplate.getExpire("key", TimeUnit.MINUTES)).thenReturn(5L);

        Long result = redisService.getExpire("key", TimeUnit.MINUTES);

        assertEquals(5L, result);
    }

    @Test
    void hSet_PutsHashValue() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        redisService.hSet("hashKey", "field", "value");

        verify(hashOperations).put("hashKey", "field", "value");
    }

    @Test
    void hGet_ReturnsHashValue() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("hashKey", "field")).thenReturn("value");

        Object result = redisService.hGet("hashKey", "field");

        assertEquals("value", result);
    }

    @Test
    void hGetAll_ReturnsEntries() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("hashKey")).thenReturn(Map.of("field", "value"));

        Map<Object, Object> result = redisService.hGetAll("hashKey");

        assertEquals(1, result.size());
        assertEquals("value", result.get("field"));
    }

    @Test
    void hDelete_RemovesFields() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete("hashKey", "field1", "field2")).thenReturn(2L);

        Long result = redisService.hDelete("hashKey", "field1", "field2");

        assertEquals(2L, result);
    }

    @Test
    void sAdd_AddsSetMembers() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("setKey", "item1", "item2")).thenReturn(2L);

        Long result = redisService.sAdd("setKey", "item1", "item2");

        assertEquals(2L, result);
    }

    @Test
    void sMembers_ReturnsMembers() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("setKey")).thenReturn(Set.of("item1", "item2"));

        Set<Object> result = redisService.sMembers("setKey");

        assertEquals(2, result.size());
    }

    @Test
    void deleteByPattern_EvictsMatchingKeys() {
        Set<String> matchedKeys = Set.of("roles::1", "roles::all");
        when(redisTemplate.keys("roles::*")).thenReturn(matchedKeys);
        when(redisTemplate.delete(matchedKeys)).thenReturn(2L);

        redisService.deleteByPattern("roles::*");

        verify(redisTemplate).keys("roles::*");
        verify(redisTemplate).delete(matchedKeys);
    }
}
