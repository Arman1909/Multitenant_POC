package com.example.pbm.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;
@Entity @Table(name = "claims") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Claim {
    @Id private UUID id;
    @Column(nullable = false) private Long tenantId;
    @Column(nullable = false) private UUID memberId;
    @Column(nullable = false) private BigDecimal amount;
    @Column(nullable = false) private String status;
}
