package com.medimpact.mt.member;

import com.medimpact.mt.common.PageResponse;
import com.medimpact.mt.group.GroupDtos.MembershipResponse;
import com.medimpact.mt.group.GroupService;
import com.medimpact.mt.member.MemberDtos.MemberRequest;
import com.medimpact.mt.member.MemberDtos.MemberResponse;
import com.medimpact.mt.security.Fe;
import com.medimpact.mt.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Permissions per endpoint are declared in {@link com.medimpact.mt.security.AccessRules}. */
@Tag(name = "Members")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final GroupService groupService;

    @Operation(summary = "Create a member (MEMBER_CREATE)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return memberService.create(request);
    }

    @Operation(summary = "Search members (MEMBER_READ)", description = "q matches member number, first/last name or email")
    @GetMapping
    public PageResponse<MemberResponse> search(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) MemberStatus status,
                                               @PageableDefault(size = 20, sort = "memberId", direction = Sort.Direction.ASC)
                                               Pageable pageable) {
        return memberService.search(q, status, pageable);
    }

    @Operation(summary = "Get a member (MEMBER_READ)")
    @GetMapping("/{memberId}")
    public MemberResponse get(@PathVariable Long memberId) {
        return memberService.get(memberId);
    }

    @Operation(summary = "Update a member (MEMBER_UPDATE)")
    @PutMapping("/{memberId}")
    public MemberResponse update(@PathVariable Long memberId, @Valid @RequestBody MemberRequest request) {
        return memberService.update(memberId, request);
    }

    @Operation(summary = "Deactivate a member - soft delete, ends group memberships (MEMBER_DEACTIVATE)")
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long memberId) {
        memberService.deactivate(memberId);
    }

    @Operation(summary = "Reactivate a member (MEMBER_REACTIVATE)")
    @PostMapping("/{memberId}/reactivate")
    public MemberResponse reactivate(@PathVariable Long memberId) {
        return memberService.reactivate(memberId);
    }

    @Operation(summary = "Groups a member belongs to (MEMBER_READ; includeHistory=true also needs AUDIT_READ)")
    @GetMapping("/{memberId}/groups")
    public List<MembershipResponse> groups(@PathVariable Long memberId,
                                           @RequestParam(defaultValue = "false") boolean includeHistory) {
        if (includeHistory) {
            Permissions.require(Fe.AUDIT_READ);
        }
        return groupService.membershipsOfMember(memberId, includeHistory);
    }
}
