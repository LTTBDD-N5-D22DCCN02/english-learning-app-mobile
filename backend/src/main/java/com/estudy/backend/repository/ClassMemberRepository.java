package com.estudy.backend.repository;

import com.estudy.backend.entity.Class;
import com.estudy.backend.entity.ClassMember;
import com.estudy.backend.entity.User;
import com.estudy.backend.enums.ClassMemberRole;
import com.estudy.backend.enums.ClassMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, UUID> {

    Optional<ClassMember> findByClazzAndUser(Class clazz, User user);

    boolean existsByClazzAndUser(Class clazz, User user);

    boolean existsByClazzAndUserAndStatus(Class clazz, User user, ClassMemberStatus status);

    List<ClassMember> findByUserAndStatus(User user, ClassMemberStatus status);

    List<ClassMember> findByClazzAndStatus(Class clazz, ClassMemberStatus status);

    List<ClassMember> findByClazz(Class clazz);

    // SỬA QUERY NÀY - Truyền cả clazz và user
    @Query("SELECT cm FROM ClassMember cm WHERE cm.user = :user AND cm.status = 'APPROVED' AND cm.clazz.deleted = false")
    List<ClassMember> findApprovedByUser(@Param("user") User user);

    @Query("SELECT cm FROM ClassMember cm WHERE cm.clazz = :clazz AND cm.status = 'PENDING'")
    List<ClassMember> findPendingByClass(@Param("clazz") Class clazz);

    Optional<ClassMember> findByClazzAndUserAndRole(Class clazz, User user, ClassMemberRole role);

    @Query("SELECT cm FROM ClassMember cm WHERE cm.clazz = :clazz AND cm.role = 'LEADER' AND cm.status = 'APPROVED'")
    Optional<ClassMember> findLeaderByClass(@Param("clazz") Class clazz);
}