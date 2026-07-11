package com.taskmind.backend.auth.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByPrimaryEmail(String primaryEmail);

    Optional<UserJpaEntity> findByPrimaryPhone(String primaryPhone);

    List<UserJpaEntity> findAllByOrderByDisplayNameAscPrimaryEmailAsc();

    List<UserJpaEntity> findByStatusOrderByDisplayNameAscPrimaryEmailAsc(AuthJpaEnums.UserStatus status);

    @Query(
            value = """
                    SELECT DISTINCT u.*
                    FROM users u
                    JOIN project_memberships visible_member ON visible_member.user_id = u.id
                    JOIN projects p ON p.id = visible_member.project_id
                    LEFT JOIN project_memberships manager_membership
                        ON manager_membership.project_id = visible_member.project_id
                        AND manager_membership.user_id = :managerUserId
                        AND manager_membership.role IN ('OWNER', 'ADMIN')
                    WHERE u.status = :status
                      AND (p.owner_user_id = :managerUserId OR manager_membership.user_id IS NOT NULL)
                    ORDER BY u.display_name ASC NULLS LAST, u.primary_email ASC NULLS LAST
                    """,
            nativeQuery = true)
    List<UserJpaEntity> findVisibleProjectUsersForManager(
            @Param("managerUserId") UUID managerUserId, @Param("status") String status);
}
