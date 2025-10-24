package com.jpmc.midascore;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {

    private final UserRepository userRepository;

    public BalanceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public double getBalanceForUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserRecord::getBalance)   // returns Float (or Number)
                .map(Number::doubleValue)      // converts Float -> double (or other Number -> double)
                .orElse(0.0);
    }

    // optional: keep for cache-based approach
    public void updateUserBalance(Long userId, double newBalance) {
        // no-op or update cache if you implement one
    }
}
