package com.realestate.security;

import com.realestate.repository.UserAccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccountSessionFilter extends OncePerRequestFilter {
    private final UserAccountRepository users;
    public AccountSessionFilter(UserAccountRepository users) { this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var session = request.getSession(false);
        if (session != null && session.getAttribute("accountId") instanceof Long id) {
            users.findById(id).filter(user -> List.of("Admin", "Manager", "Associate").contains(user.getRole()))
                .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))));
        }
        chain.doFilter(request, response);
    }
}
