package com.spendwise.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendwise.dto.error.ErrorResponse;
import com.spendwise.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;


/*
This class helps in When authentication fails, we send a structured JSON response saying:
‘Unauthorized’ instead of the default Spring error page.
This class is written to customize how your API responds when authentication fails.
Spring calls an AuthenticationEntryPoint, when: A request hits a protected endpoint and
there is no JWT or it’s invalid
*/
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper; //default ObjectMapper bean in Spring Boot. Turns our Java error object into JSON that the client can understand.”

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse)); //Converts the ErrorResponse Java object → JSON string using objectMapper.Sends the JSON in the HTTP response body
    }
}
