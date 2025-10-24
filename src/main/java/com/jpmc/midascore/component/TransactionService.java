package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRepository;

    public TransactionService(UserRepository userRepository, TransactionRecordRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void processTransaction(Transaction tx) {
        Optional<UserRecord> maybeSender = userRepository.findById(tx.getSenderId());
        Optional<UserRecord> maybeRecipient = userRepository.findById(tx.getRecipientId());

        if (maybeSender.isEmpty()) {
            System.out.println("⚠️ Sender not found for transaction: " + tx);
            return;
        }
        if (maybeRecipient.isEmpty()) {
            System.out.println("⚠️ Recipient not found for transaction: " + tx);
            return;
        }

        UserRecord sender = maybeSender.get();
        UserRecord recipient = maybeRecipient.get();

        if (sender.getBalance() < tx.getAmount()) {
            System.out.println("⚠️ Insufficient balance for sender: " + sender.getName());
            return;
        }

        sender.setBalance(sender.getBalance() - tx.getAmount());
        recipient.setBalance(recipient.getBalance() + tx.getAmount());

        userRepository.save(sender);
        userRepository.save(recipient);

        TransactionRecord record = new TransactionRecord(sender, recipient, tx.getAmount());
        transactionRepository.save(record);

        System.out.println("✅ Transaction processed and recorded: " + tx);
    }
}
