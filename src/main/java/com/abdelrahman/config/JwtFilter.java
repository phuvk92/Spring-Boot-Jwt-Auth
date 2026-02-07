package com.abdelrahman.config;

import com.abdelrahman.service.JwtService;
import com.abdelrahman.service.MyUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import io.jsonwebtoken.security.SignatureException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ApplicationContext applicationContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String token = null;
        String userEmail = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try { userEmail = jwtService.extractUsername(token);
            }catch (SignatureException ex) {
                handleJwtException(response, "Invalid JWT signature", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (ExpiredJwtException ex) {
                handleJwtException(response, "JWT token has expired", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (MalformedJwtException ex) {
                handleJwtException(response, "Invalid JWT token format", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (UnsupportedJwtException ex) {
                handleJwtException(response, "JWT token is not supported", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (IllegalArgumentException ex) {
                handleJwtException(response, "JWT token is empty or null", HttpServletResponse.SC_BAD_REQUEST);
                return;
            } catch (Exception ex) {
                handleJwtException(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }


        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = applicationContext
                    .getBean(MyUserDetailsService.class)
                    .loadUserByUsername(userEmail);

            if (jwtService.validateToken(token, userDetails)) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

            filterChain.doFilter(request, response);
    }


    private void handleJwtException(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"success\":false,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}",
                message,
                LocalDateTime.now().toString()
        );

        response.getWriter().write(jsonResponse);
    }
}