package com.medimpact.mt.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GroupRepository extends JpaRepository<MemberGroup, Long>, JpaSpecificationExecutor<MemberGroup> {

    boolean existsByGroupNumber(String groupNumber);

    boolean existsByGroupNumberAndGroupIdNot(String groupNumber, Long groupId);
}
