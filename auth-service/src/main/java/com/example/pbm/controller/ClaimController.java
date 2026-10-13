package com.example.pbm.controller;
import com.example.pbm.dto.response.ClaimResponse;
import com.example.pbm.service.ClaimService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/claims") @RequiredArgsConstructor
public class ClaimController {
    private final ClaimService svc;
    @GetMapping @PreAuthorize("hasAuthority('CLAIM_READ')") public List<ClaimResponse> list() { return svc.list(); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('CLAIM_READ')") public ClaimResponse get(@PathVariable UUID id) { return svc.get(id); }
}
