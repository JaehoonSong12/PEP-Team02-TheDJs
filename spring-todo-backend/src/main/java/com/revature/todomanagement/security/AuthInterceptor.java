package com.revature.todomanagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 1. Bypass OPTIONS (CORS preflight) requests
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 2. Read Authorization header
        String authHeader = request.getHeader("Authorization");

        // 3. Check header presence and format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain");
            response.getWriter().write("Missing or malformed Authorization header");
            return false;
        }

        // 4. Strip "Bearer " prefix to get the raw token
        String token = authHeader.substring(7);

        // 5. Extract username (subject) from token
        String username = jwtUtil.extractUsername(token);

        // 6. If username is null, token is invalid
        if (username == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain");
            response.getWriter().write("Invalid or expired token");
            return false;
        }

        // 7-8. Validate token against extracted username
        if (!jwtUtil.isTokenValid(token, username)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain");
            response.getWriter().write("Invalid or expired token");
            return false;
        }

        // 9. Extract userId claim from token
        String userId = jwtUtil.extractUserId(token);

        // 10. Set request attributes for downstream controllers
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        // 11. Allow request to proceed
        return true;
    }
}
