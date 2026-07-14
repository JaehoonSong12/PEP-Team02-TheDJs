package com.revature.todomanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthInterceptor")
class AuthInterceptorTest {

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    AuthInterceptor authInterceptor;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    Object handler = new Object();

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("CORS preflight bypass")
    class CorsPreflight {

        @Test
        @DisplayName("OPTIONS request returns true without header inspection")
        void preHandle_optionsRequest_returnsTrueWithoutHeaderInspection() throws Exception {
            request.setMethod("OPTIONS");

            boolean result = authInterceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
            verifyNoInteractions(jwtUtil);
        }
    }

    @Nested
    @DisplayName("Header validation")
    class HeaderValidation {

        @Test
        @DisplayName("No Authorization header returns 401")
        void preHandle_noAuthHeader_returns401() throws Exception {
            request.setMethod("GET");

            boolean result = authInterceptor.preHandle(request, response, handler);

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Non-Bearer Authorization header returns 401")
        void preHandle_nonBearerHeader_returns401() throws Exception {
            request.setMethod("GET");
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            boolean result = authInterceptor.preHandle(request, response, handler);

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("Token validation")
    class TokenValidation {

        @Test
        @DisplayName("extractUsername returns null gives 401")
        void preHandle_extractUsernameReturnsNull_returns401() throws Exception {
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer sometoken");
            when(jwtUtil.extractUsername("sometoken")).thenReturn(null);

            boolean result = authInterceptor.preHandle(request, response, handler);

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("isTokenValid returns false gives 401")
        void preHandle_isTokenValidReturnsFalse_returns401() throws Exception {
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer sometoken");
            when(jwtUtil.extractUsername("sometoken")).thenReturn("testuser");
            when(jwtUtil.isTokenValid("sometoken", "testuser")).thenReturn(false);

            boolean result = authInterceptor.preHandle(request, response, handler);

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Passes extracted username to isTokenValid (correct wiring)")
        void preHandle_validToken_passesExtractedUsernameToIsTokenValid() throws Exception {
            request.setMethod("GET");
            String token = "valid-token-abc";
            String extractedUsername = "wireduser";
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtUtil.extractUsername(token)).thenReturn(extractedUsername);
            when(jwtUtil.isTokenValid(token, extractedUsername)).thenReturn(false);

            authInterceptor.preHandle(request, response, handler);

            verify(jwtUtil).isTokenValid(token, extractedUsername);
        }
    }

    @Nested
    @DisplayName("Successful authentication")
    class SuccessfulAuth {

        private static final String TOKEN = "valid-token";
        private static final String USERNAME = "testuser";
        private static final String USER_ID = UUID.randomUUID().toString();

        @BeforeEach
        void setUpValidRequest() {
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer " + TOKEN);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn(USERNAME);
            when(jwtUtil.isTokenValid(TOKEN, USERNAME)).thenReturn(true);
            when(jwtUtil.extractUserId(TOKEN)).thenReturn(USER_ID);
        }

        @Test
        @DisplayName("Sets userId attribute on request")
        void preHandle_validToken_setsUserIdAttribute() throws Exception {
            authInterceptor.preHandle(request, response, handler);

            assertThat(request.getAttribute("userId")).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Sets username attribute on request")
        void preHandle_validToken_setsUsernameAttribute() throws Exception {
            authInterceptor.preHandle(request, response, handler);

            assertThat(request.getAttribute("username")).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("Returns true for valid token")
        void preHandle_validToken_returnsTrue() throws Exception {
            boolean result = authInterceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Rejection side-effects")
    class RejectionSideEffects {

        @Test
        @DisplayName("No attributes set on rejection")
        void preHandle_rejection_noAttributesSet() throws Exception {
            request.setMethod("GET");
            // No Authorization header — triggers rejection

            authInterceptor.preHandle(request, response, handler);

            assertThat(request.getAttribute("userId")).isNull();
            assertThat(request.getAttribute("username")).isNull();
        }
    }
}
