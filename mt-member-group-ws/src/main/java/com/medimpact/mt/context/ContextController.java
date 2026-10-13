package com.medimpact.mt.context;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Context")
@RestController
@RequestMapping("/api/v1")
public class ContextController {

    public record ContextResponse(CallerContext caller, List<String> authorities) {
    }

    @Operation(summary = "Show what this service resolved from your JWT (debugging aid)")
    @GetMapping("/context")
    public ContextResponse context(Authentication authentication) {
        return new ContextResponse(CallerContextHolder.require(),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    }
}
