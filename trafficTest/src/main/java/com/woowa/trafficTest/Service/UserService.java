package com.woowa.trafficTest.Service;

import com.woowa.trafficTest.Entity.User;
import com.woowa.trafficTest.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public int processUsers(int count) {
        List<User> users = new ArrayList<>();
        StringBuilder garbageLog = new StringBuilder(); // L1(GC) 문제를 일으킬 리스트
        List<User> usersBefore = userRepository.findAllByOrderByIdAsc();

        for (long i = 1; i <= count; i++) {
            User user = usersBefore.get((int) i);

            if (user != null) {
                users.add(user);

                garbageLog.append(user.makeLog()).append("\n");
            }
        }
        return users.size();
    }
}