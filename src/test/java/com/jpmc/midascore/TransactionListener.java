package com.jpmc.midascore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class TransactionListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRecordRepository transactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-group")
    public void listen(String transactionJson) {
        try {
            Transaction transaction = objectMapper.readValue(transactionJson, Transaction.class);
            System.out.println("Received transaction: " + transaction);

            // Using Optional since standard JpaRepository#findById returns Optional<T>
            Optional<UserRecord> maybeSender = userRepository.findById(transaction.getSenderId());
            Optional<UserRecord> maybeRecipient = userRepository.findById(transaction.getRecipientId());

            if (maybeSender.isEmpty() || maybeRecipient.isEmpty()) {
                System.out.println("❌ Invalid sender or recipient");
                return;
            }

            UserRecord sender = maybeSender.get();
            UserRecord recipient = maybeRecipient.get();

            if (sender.getBalance() < transaction.getAmount()) {
                System.out.println("❌ Insufficient balance for sender: " + sender.getName());
                return;
            }

            // Call Incentive API (if your incentive service expects different payload adjust accordingly)
            Incentive incentive = null;
            try {
                incentive = restTemplate.postForObject(
                        "http://localhost:8080/incentive",
                        transaction,
                        Incentive.class
                );
            } catch (Exception apiEx) {
                // Don't fail the whole flow if the incentive service is down; log and continue with 0 incentive
                System.out.println("⚠️ Incentive service call failed: " + apiEx.getMessage());
            }

            Float incentiveAmount = (incentive != null) ? incentive.getAmount() : 0.0f;

            // Update balances (apply incentive to recipient)
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

            userRepository.save(sender);
            userRepository.save(recipient);

            // Create and save transaction record using the existing constructor and set incentive
            TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount());
            record.setIncentive(incentiveAmount); // nullable Float field
            transactionRepository.save(record);

            System.out.printf("✅ Transaction recorded with incentive %.2f%n", incentiveAmount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
