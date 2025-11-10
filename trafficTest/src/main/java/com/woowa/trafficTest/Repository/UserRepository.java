package com.woowa.trafficTest.Repository;

import com.woowa.trafficTest.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}