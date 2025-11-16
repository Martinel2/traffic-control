package com.woowa.trafficTest.Repository;

import com.woowa.trafficTest.Entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllByOrderByIdAsc();
    List<User> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);
}