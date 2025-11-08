package com.simple_online_store_backend.controller.helper;

import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.Map;

@ConditionalOnProperty(value = "demo.helpers.enabled", havingValue = "true")
@RestController
@RequestMapping("/auth/dev")
public class AccountDemoController {

    private final PeopleService peopleService;

    public AccountDemoController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @Operation(
            summary = "Lock user account (demo-only)",
            description = """
        Sets the user's account to locked = true.  
        Used to simulate 423 Locked errors in protected endpoints.

        ### How to test
        1. Login as a valid user → get `access_token` in `/auth/login`.
        2. Authorize with that token in Swagger.
        3. Call `POST /auth/dev/_lock?username=<username>` — you'll see `"locked": true` in the response.
        4. Now try any protected endpoint (e.g., `/address/add-address`) → you'll get **423 ACCOUNT_LOCKED**.
        5. To unlock, call `/auth/dev/_unlock?username=<username>`.

        **Notes:**
        - Dev helper endpoints are available only when `demo.helpers.enabled=true`.
        - The lock flag is toggled at the DB level; after unlock, the user can call endpoints again.
        """
    )
    @PostMapping("/_lock")
    public Map<String, Object> lock(
            @Parameter(
                    name = "username",
                    description = "Username to unlock (default: 'user')",
                    required = true,
                    example = "user"
            )
            @RequestParam String username) {
        var state = peopleService.setLocked(username, true);
        return Map.of("username", username, "locked", state);
    }

    @Operation(
            summary = "Unlock user account (demo-only)",
            description = """
        Sets the user's account to locked = false.  
        Used to restore access after testing 423 errors.

        ### How to test
        1. Call `/auth/dev/_lock?username=<username>` to lock the account.
        2. Call any protected endpoint (e.g., `/address/add-address`) → you'll get 423.
        3. Then call `/auth/dev/_unlock?username=<username>` — you'll see `"locked": false`.
        4. Retry the protected endpoint → it works again.

        **Notes:**
        - Only for demo/testing; in production, lock/unlock is handled by admins.
        - Available only when `demo.helpers.enabled=true`.
        """
    )
    @PostMapping("/_unlock")
    public Map<String, Object> unlock(
            @Parameter(
                    name = "username",
                    description = "Username to unlock (default: 'user')",
                    required = true,
                    example = "user"
            )
            @RequestParam String username) {
        var state = peopleService.setLocked(username, false);
        return Map.of("username", username, "locked", state);
    }
}

