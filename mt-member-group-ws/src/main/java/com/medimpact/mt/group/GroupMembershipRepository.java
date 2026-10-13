package com.medimpact.mt.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    @Query("select gm from GroupMembership gm where gm.member.memberId = :memberId and gm.endDate is null")
    List<GroupMembership> findOpenByMemberId(@Param("memberId") Long memberId);

    @Query("select gm from GroupMembership gm where gm.group.groupId = :groupId and gm.endDate is null")
    List<GroupMembership> findOpenByGroupId(@Param("groupId") Long groupId);

    @Query("""
            select gm from GroupMembership gm
            where gm.group.groupId = :groupId and gm.member.memberId = :memberId and gm.endDate is null""")
    Optional<GroupMembership> findOpen(@Param("groupId") Long groupId, @Param("memberId") Long memberId);

    @Query("""
            select gm from GroupMembership gm join fetch gm.member join fetch gm.group
            where gm.group.groupId = :groupId and (:includeHistory = true or gm.endDate is null)
            order by gm.member.lastName, gm.member.firstName, gm.startDate""")
    List<GroupMembership> findByGroup(@Param("groupId") Long groupId, @Param("includeHistory") boolean includeHistory);

    @Query("""
            select gm from GroupMembership gm join fetch gm.member join fetch gm.group
            where gm.member.memberId = :memberId and (:includeHistory = true or gm.endDate is null)
            order by gm.group.groupName, gm.startDate""")
    List<GroupMembership> findByMember(@Param("memberId") Long memberId, @Param("includeHistory") boolean includeHistory);
}
