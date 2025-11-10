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
        List<String> garbageLog = new ArrayList<>(); // L1(GC) 문제를 일으킬 리스트

        for (long i = 1; i <= count; i++) {
            // [L2 문제]: 루프 안에서 N+1 쿼리 발생
            User user = userRepository.findById(i).orElse(null);

            if (user != null) {
                users.add(user);

                // [L1 문제]: 매번 불필요한 객체를 생성하여 GC 유발
                String log = user.makeLog();
                garbageLog.add(log);
            }
        }
        return users.size();
    }
}