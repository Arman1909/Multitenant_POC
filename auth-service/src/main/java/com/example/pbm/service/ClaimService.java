package com.example.pbm.service;
import com.example.pbm.dto.response.ClaimResponse;
import com.example.pbm.repository.ClaimRepository;
import com.example.pbm.tenant.TenantContext;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class ClaimService {
    private final ClaimRepository repo;
    private final TenantContext ctx;
    public List<ClaimResponse> list() { return repo.findByTenantId(ctx.requireActiveTenant()).stream().map(ClaimResponse::from).toList(); }
    public ClaimResponse get(UUID id) {
        return repo.findByIdAndTenantId(id, ctx.requireActiveTenant()).map(ClaimResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }
}
