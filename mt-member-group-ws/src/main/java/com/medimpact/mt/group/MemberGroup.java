package com.medimpact.mt.group;

import com.medimpact.mt.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** "group" is a reserved word in SQL, hence member_group. */
@Getter
@Setter
@Entity
@Table(name = "member_group")
public class MemberGroup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_number", nullable = false, unique = true, length = 50)
    private String groupNumber;

    @Column(name = "group_name", nullable = false, length = 150)
    private String groupName;

    @Column(name = "group_desc", length = 500)
    private String groupDesc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GroupStatus status;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;
}
