package com.example.tokenservice;

import com.example.tokenservice.dto.CreateTokenRequest;
import com.example.tokenservice.dto.TokenResponse;
import com.example.tokenservice.repository.TokenVaultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TokenServiceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("payments_db")
            .withUsername("root")
            .withPassword("password")
            .withReuse(true);
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TokenVaultRepository tokenVaultRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }
    
    @Test
    void testCreateToken_Success() {
        CreateTokenRequest request = CreateTokenRequest.builder()
                .userId(UUID.randomUUID())
                .paymentData("4111111111111111")
                .dataType("CARD")
                .expiryTime(LocalDateTime.now().plusDays(1))
                .maxUsageLimit(5)
                .build();
        
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/api/v1/tokens", request, TokenResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTokenId()).isNotBlank();
        assertThat(response.getBody().getUserId()).isEqualTo(request.getUserId());
        assertThat(response.getBody().getDataType()).isEqualTo("CARD");
        assertThat(response.getBody().getStatus()).isEqualTo("ACTIVE");
    }
    
    @Test
    void testGetToken_Success() {
        // First create a token
        CreateTokenRequest createRequest = CreateTokenRequest.builder()
                .userId(UUID.randomUUID())
                .paymentData("upi://pay?pa=9876543210@paytm")
                .dataType("UPI")
                .build();
        
        ResponseEntity<TokenResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/tokens", createRequest, TokenResponse.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tokenId = createResponse.getBody().getTokenId();
        
        // Then retrieve the token
        ResponseEntity<TokenResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/tokens/" + tokenId, TokenResponse.class);
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getTokenId()).isEqualTo(tokenId);
        assertThat(getResponse.getBody().getMaskedData()).isNotNull();
    }
    
    @Test
    void testRevokeToken_Success() {
        // First create a token
        CreateTokenRequest createRequest = CreateTokenRequest.builder()
                .userId(UUID.randomUUID())
                .paymentData("1234567890123456")
                .dataType("BANK")
                .build();
        
        ResponseEntity<TokenResponse> createResponse = restTemplate.postForEntity(
                "/api/v1/tokens", createRequest, TokenResponse.class);
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tokenId = createResponse.getBody().getTokenId();
        
        // Then revoke the token
        ResponseEntity<Void> revokeResponse = restTemplate.exchange(
                "/api/v1/tokens/" + tokenId,
                HttpMethod.DELETE,
                null,
                Void.class);
        
        assertThat(revokeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
    
    @Test
    void testGetUserTokens_Success() {
        UUID userId = UUID.randomUUID();
        
        // Create multiple tokens for the same user
        CreateTokenRequest request1 = CreateTokenRequest.builder()
                .userId(userId)
                .paymentData("4111111111111111")
                .dataType("CARD")
                .build();
        
        CreateTokenRequest request2 = CreateTokenRequest.builder()
                .userId(userId)
                .paymentData("upi://pay?pa=9876543210@paytm")
                .dataType("UPI")
                .build();
        
        restTemplate.postForEntity("/api/v1/tokens", request1, TokenResponse.class);
        restTemplate.postForEntity("/api/v1/tokens", request2, TokenResponse.class);
        
        // Retrieve user tokens
        ResponseEntity<TokenResponse[]> response = restTemplate.getForEntity(
                "/api/v1/tokens/user/" + userId, TokenResponse[].class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }
}
