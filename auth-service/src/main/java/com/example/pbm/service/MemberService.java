package com.example.pbm.service;
import com.example.pbm.dto.response.MemberResponse;
import com.example.pbm.repository.MemberRepository;
import com.example.pbm.tenant.TenantContext;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class MemberService {
    private final MemberRepository repo;
    private final TenantContext ctx;
    public List<MemberResponse> list() { return repo.findByTenantId(ctx.requireActiveTenant()).stream().map(MemberResponse::from).toList(); }
    public MemberResponse get(UUID id) {
        return repo.findByIdAndTenantId(id, ctx.requireActiveTenant()).map(MemberResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
    }
}
