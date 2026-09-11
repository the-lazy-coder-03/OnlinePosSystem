package org.example.onlinepossystem.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.onlinepossystem.security.api.RequestClientIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoggingAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAuthenticationFailureHandler.class);

    public LoggingAuthenticationFailureHandler() {
        super("/login?error");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        logger.warn("Failed login attempt from IP {}", RequestClientIp.resolve(request));

        if ("true".equals(request.getParameter("adminLogin"))) {
            getRedirectStrategy().sendRedirect(request, response, "/admin/login?error");
            return;
        }

        super.onAuthenticationFailure(request, response, exception);
    }

}
