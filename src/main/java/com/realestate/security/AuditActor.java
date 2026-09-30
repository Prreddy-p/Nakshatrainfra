package com.realestate.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class AuditActor {
    private AuditActor() {}
    public static String name() {
        if (RecordAccess.user() != null) return RecordAccess.user().getName();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var session = attributes.getRequest().getSession(false);
            if (session != null && session.getAttribute("auditUserName") instanceof String name) return name;
        }
        return null;
    }
    public static void signIn(com.realestate.model.UserAccount user) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var request = attributes.getRequest();
            if (request.getSession(false) != null) request.changeSessionId();
            else request.getSession(true);
            request.getSession().setAttribute("auditUserName", user.getName());
            request.getSession().setAttribute("accountId", user.getId());
        }
    }
    public static void signOut() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var session = attributes.getRequest().getSession(false);
            if (session != null) session.invalidate();
        }
    }
}
