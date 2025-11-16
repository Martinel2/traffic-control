package com.woowa.trafficTest.Service;

import com.woowa.trafficTest.Entity.User;
import com.woowa.trafficTest.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private static final Logger userLogger = LoggerFactory.getLogger("USER_LOG");

    public int processUsers(int count) {
        int total = 0;
        long lastId = 1L;

        while (total < count) {
            List<User> pageUsers = getUsers(count, total, lastId);
            if (pageUsers.isEmpty()) {
                break;
            }

            processLog(pageUsers);
            lastId += pageUsers.size();
            total += pageUsers.size();
        }

        return total;
    }

    private void processLog(List<User> users) {
        for (User user : users) {
            userLogger.info(user.makeLog());
        }
    }

    private List<User> getUsers(int count, int totalProcessedCount, long lastSeenId) {
        int remainingCount = count - totalProcessedCount;
        int MAX_PROCESS_SIZE = 25;
        int currentChunkSize = Math.min(MAX_PROCESS_SIZE, remainingCount);

        Pageable pageable = PageRequest.of(0, currentChunkSize);
        return userRepository.findByIdGreaterThanOrderByIdAsc(lastSeenId, pageable);
    }
}