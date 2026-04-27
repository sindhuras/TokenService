package com.example.tokenservice.unit;

import com.example.tokenservice.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ApiResponseTest {
    
    @Test
    void testSuccessWithData() {
        String testData = "test data";
        ApiResponse<String> response = ApiResponse.success(testData);
        
        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertEquals(testData, response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithMessageAndData() {
        String message = "Custom success message";
        String testData = "test data";
        ApiResponse<String> response = ApiResponse.success(message, testData);
        
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(testData, response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithMessage() {
        String errorMessage = "Error occurred";
        ApiResponse<String> response = ApiResponse.error(errorMessage);
        
        assertFalse(response.isSuccess());
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithCodeAndMessage() {
        String errorCode = "ERROR_CODE";
        String errorMessage = "Error occurred";
        ApiResponse<String> response = ApiResponse.error(errorCode, errorMessage);
        
        assertFalse(response.isSuccess());
        assertEquals(errorMessage, response.getMessage());
        assertEquals(errorCode, response.getErrorCode());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithNullData() {
        ApiResponse<String> response = ApiResponse.success(null);
        
        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithNullMessage() {
        ApiResponse<String> response = ApiResponse.error((String) null);
        
        assertFalse(response.isSuccess());
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testErrorWithNullCodeAndMessage() {
        ApiResponse<String> response = ApiResponse.error(null, null);
        
        assertFalse(response.isSuccess());
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testBuilderPattern() {
        String testData = "test data";
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Custom message")
                .data(testData)
                .errorCode("CODE")
                .timestamp(LocalDateTime.now())
                .build();
        
        assertTrue(response.isSuccess());
        assertEquals("Custom message", response.getMessage());
        assertEquals(testData, response.getData());
        assertEquals("CODE", response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testAllArgsConstructor() {
        String message = "Test message";
        String data = "test data";
        String errorCode = "TEST_ERROR";
        LocalDateTime timestamp = LocalDateTime.now();
        
        ApiResponse<String> response = new ApiResponse<>(true, message, data, errorCode, timestamp);
        
        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
        assertEquals(errorCode, response.getErrorCode());
        assertEquals(timestamp, response.getTimestamp());
    }
    
    @Test
    void testNoArgsConstructor() {
        ApiResponse<String> response = new ApiResponse<>();
        
        assertNull(response.isSuccess());
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getErrorCode());
        assertNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithComplexData() {
        java.util.List<String> testData = java.util.List.of("item1", "item2", "item3");
        ApiResponse<java.util.List<String>> response = ApiResponse.success(testData);
        
        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertEquals(testData, response.getData());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testSuccessWithEmptyData() {
        java.util.List<String> testData = java.util.Collections.emptyList();
        ApiResponse<java.util.List<String>> response = ApiResponse.success(testData);
        
        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertEquals(testData, response.getData());
        assertTrue(response.getData().isEmpty());
        assertNull(response.getErrorCode());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testTimestampIsSet() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ApiResponse<String> response = ApiResponse.success("test");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        
        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().isAfter(before));
        assertTrue(response.getTimestamp().isBefore(after));
    }
}
