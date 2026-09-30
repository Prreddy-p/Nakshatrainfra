package com.realestate.security;

import com.realestate.model.OwnedRecord;
import com.realestate.model.UserAccount;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

public final class RecordAccess {
    private RecordAccess() {}
    public static UserAccount user() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserAccount user ? user : null;
    }
    public static Long userId() { return user() == null ? null : user().getId(); }
    public static boolean canSee(OwnedRecord record) {
        UserAccount user = user();
        return user != null && ("Admin".equals(user.getRole()) || "Manager".equals(user.getRole())
            || ("Associate".equals(user.getRole()) && user.getId().equals(record.getCreatedByUserId())));
    }
    public static <T extends OwnedRecord> T require(T record) {
        if (!canSee(record)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        return record;
    }
    public static <T extends OwnedRecord> List<T> visible(List<T> records) {
        return records.stream().filter(RecordAccess::canSee).toList();
    }
}
