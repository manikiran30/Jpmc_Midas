package com.jpmc.midascore;

import com.jpmc.midascore.foundation.Balance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class BalanceController {

    @Autowired
    private BalanceService balanceService;

    @GetMapping("/balance")
    public Balance getUserBalance(@RequestParam Long userId) {
        // Get the balance from service
        double amount = balanceService.getBalanceForUser(userId);
        return new Balance(userId, amount);
    }
}
