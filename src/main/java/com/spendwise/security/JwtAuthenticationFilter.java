package com.spendwise.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendwise.dto.error.ErrorResponse;
import com.spendwise.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil; //handles JWT validation & claims extraction
    private final UserDetailsService userDetailsService; //to load user from db
    private final ObjectMapper objectMapper; //converts Java objects to JSON for error responses

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, //This method runs once for every incoming HTTP request.
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkip(request)) { // If the request is for login/registration endpoints, skip authentication.
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER); // A header is extra information sent along with an HTTP response. Bearer <token>
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) { //"Did the client send a valid Authorization header with Bearer token?" If NOT → ignore and continue.
            filterChain.doFilter(request, response);
            return;
        }

        //Removes "Bearer " from the header. Extracts the real JWT. If no token exists → skip authentication
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }


        //Now we just have plain token from which claims can be extracted so here we are validating it with jwtUtil methods for expiration and signature
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT: token present but invalid or expired (prefix: {}...)", maskToken(token));
            sendUnauthorized(request, response, ErrorCode.INVALID_TOKEN);
            return;
        }

        //extarcting all other claims from jwtUtil methods
        Optional<TokenClaims> claimsOpt = jwtUtil.extractClaims(token);
        if (claimsOpt.isEmpty()) {
            log.warn("Invalid JWT: could not extract claims (prefix: {}...)", maskToken(token));
            sendUnauthorized(request, response, ErrorCode.INVALID_TOKEN);
            return;
        }


        //VERY IMPORTANT(NOTES)
        TokenClaims claims = claimsOpt.get(); //Getting the data from the token so we know who the user is.
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.username()); //“Find the user in our system who matches the username in the token.” JWT contains username, but we verify it against the database to ensure the user still exists and get roles.
            log.debug("JWT authentication successful: userId={}, email={}", userDetails.getUsername(), userDetails.getUsername());
            UsernamePasswordAuthenticationToken authentication =           // This token(object) is for passing to UsernamePasswordAuthentication filter which is next filter after JWT filter
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); //Attach request info to the authentication token for additional context.
            SecurityContextHolder.getContext().setAuthentication(authentication); //sets the currently authenticated user in Spring Security tells Spring Security: From now on, the current thread/request is authenticated as this user.
            filterChain.doFilter(request, response); // continuing filter chain
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            log.warn("User not found for JWT claims: {}", claims.username(), e);
            sendUnauthorized(request, response, ErrorCode.UNAUTHORIZED);
        }
    }


    //For registration and login no need of token
    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/auth/");
    }


    //This method safely logs only a small part of the JWT token to help debugging without exposing the full token.
    private static String maskToken(String token) {
        if (token == null || token.length() < 8) { //atleast 8 because <bearer>
            return "***";
        }
        return token.substring(0, Math.min(8, token.length()));
    }

    //“This handles failed JWT validation gracefully, sending clear messages to clients.”
    private void sendUnauthorized(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
