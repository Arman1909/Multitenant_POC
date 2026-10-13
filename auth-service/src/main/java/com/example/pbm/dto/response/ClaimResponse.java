package com.example.pbm.dto.response;
import com.example.pbm.entity.Claim;
import java.math.BigDecimal;
import java.util.UUID;
public record ClaimResponse(UUID id, Long tenantId, UUID memberId, BigDecimal amount, String status) {
    public static ClaimResponse from(Claim c) { return new ClaimResponse(c.getId(), c.getTenantId(), c.getMemberId(), c.getAmount(), c.getStatus()); }
}
