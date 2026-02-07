package com.abdelrahman.repository;

import com.abdelrahman.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    @Query("SELECT u FROM User u WHERE u.age >= :age AND u.status = :status")
    List<User> findActiveUsersAboveAge(@Param("age") Integer age,
                                       @Param("status") User.UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    Long countActiveUsers();
}