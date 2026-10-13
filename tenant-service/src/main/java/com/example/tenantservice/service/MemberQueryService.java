package com.example.tenantservice.service;

import com.example.tenantservice.dto.MemberResponse;
import com.example.tenantservice.exception.InvalidDeRoleException;
import com.example.tenantservice.exception.MemberNotFoundException;
import com.example.tenantservice.repository.MemberViewRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MemberQueryService {

    // Fail fast client-side for an obviously malformed role, e.g. blank or
    // no underscore-delimited suffix. The DB-side check in set_user_context
    // is the authoritative one - this is just a cheap early rejection.
    private static final Pattern DE_ROLE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]*(_[A-Za-z0-9]+)+$");

    private final MemberViewRepository memberViewRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(String deRole) {
        applyDeRoleContext(deRole);
        return memberViewRepository.findAllByOrderByMemberIdAsc().stream()
                .map(MemberResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId, String deRole) {
        applyDeRoleContext(deRole);
        // A row hidden by RLS is indistinguishable from a missing row -> 404
        return memberViewRepository.findByMemberId(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    /** Runs set_user_context('DE_LAKER_MBR') on the exact connection the following query uses. */
    private void applyDeRoleContext(String deRole) {
        if (deRole == null || deRole.isBlank() || !DE_ROLE_PATTERN.matcher(deRole.trim()).matches()) {
            throw new InvalidDeRoleException(deRole);
        }

        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("CALL set_user_context(?)")) {
                ps.setString(1, deRole.trim());
                ps.execute();
            }
        });
    }
}