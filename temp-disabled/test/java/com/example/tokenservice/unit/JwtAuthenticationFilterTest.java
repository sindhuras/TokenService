package com.example.tokenservice.unit;

import com.example.tokenservice.auth.service.AuthService;
import com.example.tokenservice.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {
    
    @Mock
    private AuthService authService;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private UUID userId;
    private String validToken;
    private String invalidToken;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        validToken = "Bearer valid_token_123";
        invalidToken = "invalid_token_123";
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void testDoFilterInternalValidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(validToken);
        when(authService.validateToken("valid_token_123")).thenReturn(userId);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        UsernamePasswordAuthenticationToken authentication = 
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        
        assertEquals(userId, authentication.getPrincipal());
        assertEquals(1, authentication.getAuthorities().size());
        assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());
        
        verify(authService).validateToken("valid_token_123");
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testDoFilterInternalNoToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authService, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testDoFilterInternalEmptyToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("");
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authService, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testDoFilterInternalInvalidFormat() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(invalidToken);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authService, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void testDoFilterInternalInvalidToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        when(authService.validateToken("invalid_token")).thenThrow(new RuntimeException("Invalid token"));
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authService).validateToken("invalid_token");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response.getWriter()).write("{\"error\":\"Unauthorized\",\"message\":\"Invalid token\"}");
        verify(filterChain, never()).doFilter(request, response);
    }
    
    @Test
    void testExtractTokenBearer() {
        when(request.getHeader("Authorization")).thenReturn(validToken);
        
        String result = jwtAuthenticationFilter.extractToken(request);
        
        assertEquals("valid_token_123", result);
    }
    
    @Test
    void testExtractTokenNull() {
        when(request.getHeader("Authorization")).thenReturn(null);
        
        String result = jwtAuthenticationFilter.extractToken(request);
        
        assertNull(result);
    }
    
    @Test
    void testExtractTokenEmpty() {
        when(request.getHeader("Authorization")).thenReturn("");
        
        String result = jwtAuthenticationFilter.extractToken(request);
        
        assertNull(result);
    }
    
    @Test
    void testExtractTokenInvalidFormat() {
        when(request.getHeader("Authorization")).thenReturn(invalidToken);
        
        String result = jwtAuthenticationFilter.extractToken(request);
        
        assertNull(result);
    }
    
    @Test
    void testExtractTokenOnlyBearer() {
        when(request.getHeader("Authorization")).thenReturn("Bearer");
        
        String result = jwtAuthenticationFilter.extractToken(request);
        
        assertEquals("", result);
    }
    
    @Test
    void testDoFilterInternalWithIOException() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        when(authService.validateToken("invalid_token")).thenThrow(new RuntimeException("Invalid token"));
        when(response.getWriter()).thenThrow(new IOException("Writer error"));
        
        assertThrows(IOException.class, () -> 
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain));
        
        verify(authService).validateToken("invalid_token");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
    
    @Test
    void testDoFilterInternalWithServletException() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(validToken);
        when(authService.validateToken("valid_token_123")).thenReturn(userId);
        doThrow(new ServletException("Servlet error")).when(filterChain).doFilter(request, response);
        
        assertThrows(ServletException.class, () -> 
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain));
        
        verify(authService).validateToken("valid_token_123");
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
