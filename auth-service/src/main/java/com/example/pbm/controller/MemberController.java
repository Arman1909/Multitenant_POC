package com.example.pbm.controller;
import com.example.pbm.dto.response.MemberResponse;
import com.example.pbm.service.MemberService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/members") @RequiredArgsConstructor
public class MemberController {
    private final MemberService svc;
    @GetMapping @PreAuthorize("hasAuthority('MEMBER_READ')") public List<MemberResponse> list() { return svc.list(); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('MEMBER_READ')") public MemberResponse get(@PathVariable UUID id) { return svc.get(id); }
}
