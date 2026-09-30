package com.realestate.controller;

import com.realestate.model.UserAccount;
import com.realestate.repository.UserAccountRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

// Test-only request fixtures. Production authentication remains fully enabled.
public final class AuthenticatedRequests {
    private static final ThreadLocal<MockHttpSession> session = new ThreadLocal<>();
    public static void manager(UserAccountRepository users) {
        UserAccount user = new UserAccount();
        user.setName("Test Manager"); user.setEmailId("test-manager-" + java.util.UUID.randomUUID() + "@example.com");
        user.setPassword("unused"); user.setRole("Manager"); users.save(user);
        var value = new MockHttpSession(); value.setAttribute("accountId", user.getId()); session.set(value);
    }
    public static MockHttpServletRequestBuilder get(String path) { return MockMvcRequestBuilders.get(path).session(session.get()); }
    public static MockHttpServletRequestBuilder post(String path) { return path.startsWith("/api/auth/") ? MockMvcRequestBuilders.post(path) : MockMvcRequestBuilders.post(path).session(session.get()); }
    public static MockHttpServletRequestBuilder put(String path) { return MockMvcRequestBuilders.put(path).session(session.get()); }
    public static MockHttpServletRequestBuilder delete(String path) { return MockMvcRequestBuilders.delete(path).session(session.get()); }
}
