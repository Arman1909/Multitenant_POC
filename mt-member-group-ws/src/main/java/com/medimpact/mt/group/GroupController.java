package com.medimpact.mt.group;

import com.medimpact.mt.common.PageResponse;
import com.medimpact.mt.group.GroupDtos.AddMemberRequest;
import com.medimpact.mt.group.GroupDtos.GroupRequest;
import com.medimpact.mt.group.GroupDtos.GroupResponse;
import com.medimpact.mt.group.GroupDtos.MembershipResponse;
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
@Tag(name = "Groups")
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "Create a group (GROUP_CREATE)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody GroupRequest request) {
        return groupService.create(request);
    }

    @Operation(summary = "Search groups (GROUP_READ)", description = "q matches group number or name")
    @GetMapping
    public PageResponse<GroupResponse> search(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) GroupStatus status,
                                              @PageableDefault(size = 20, sort = "groupId", direction = Sort.Direction.ASC)
                                              Pageable pageable) {
        return groupService.search(q, status, pageable);
    }

    @Operation(summary = "Get a group (GROUP_READ)")
    @GetMapping("/{groupId}")
    public GroupResponse get(@PathVariable Long groupId) {
        return groupService.get(groupId);
    }

    @Operation(summary = "Update a group (GROUP_UPDATE)")
    @PutMapping("/{groupId}")
    public GroupResponse update(@PathVariable Long groupId, @Valid @RequestBody GroupRequest request) {
        return groupService.update(groupId, request);
    }

    @Operation(summary = "Deactivate a group - soft delete, ends every membership in it (GROUP_DEACTIVATE)")
    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long groupId) {
        groupService.deactivate(groupId);
    }

    @Operation(summary = "Reactivate a group (GROUP_REACTIVATE)")
    @PostMapping("/{groupId}/reactivate")
    public GroupResponse reactivate(@PathVariable Long groupId) {
        return groupService.reactivate(groupId);
    }

    @Operation(summary = "Members of a group (GROUP_READ; includeHistory=true also needs AUDIT_READ)")
    @GetMapping("/{groupId}/members")
    public List<MembershipResponse> members(@PathVariable Long groupId,
                                            @RequestParam(defaultValue = "false") boolean includeHistory) {
        if (includeHistory) {
            Permissions.require(Fe.AUDIT_READ);
        }
        return groupService.membersOfGroup(groupId, includeHistory);
    }

    @Operation(summary = "Add a member to a group (GROUP_MEMBER_MANAGE)")
    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse addMember(@PathVariable Long groupId, @Valid @RequestBody AddMemberRequest request) {
        return groupService.addMember(groupId, request);
    }

    @Operation(summary = "Remove a member from a group - ends the current membership (GROUP_MEMBER_MANAGE)")
    @DeleteMapping("/{groupId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        groupService.removeMember(groupId, memberId);
    }
}
