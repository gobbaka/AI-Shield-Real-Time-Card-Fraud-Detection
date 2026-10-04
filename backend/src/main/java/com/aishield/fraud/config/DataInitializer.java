package com.aishield.fraud.config;

import com.aishield.fraud.entity.*;
import com.aishield.fraud.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final FraudRuleRepository ruleRepository;
    private final SystemSettingsRepository settingsRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository alertRepository;
    private final TransactionVerificationRepository verificationRepository;
    private final TravelNoticeRepository travelNoticeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, 
                           CardRepository cardRepository, 
                           FraudRuleRepository ruleRepository, 
                           SystemSettingsRepository settingsRepository, 
                           TransactionRepository transactionRepository, 
                           FraudAlertRepository alertRepository, 
                           TransactionVerificationRepository verificationRepository,
                           TravelNoticeRepository travelNoticeRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.ruleRepository = ruleRepository;
        this.settingsRepository = settingsRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.verificationRepository = verificationRepository;
        this.travelNoticeRepository = travelNoticeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Ensure core demo accounts exist
        if (userRepository.findByEmail("admin@aishield.com").isEmpty()) {
            userRepository.save(new UserEntity(
                    "AI Shield Admin", "admin@aishield.com", passwordEncoder.encode("admin123"), "ADMIN", 
                    "+919800000001", "Hyderabad, India", "AI Shield Lead Fraud Operations & System Administrator"
            ));
        }

        if (userRepository.findByEmail("customer@aishield.com").isEmpty()) {
            UserEntity demoCustomer = userRepository.save(new UserEntity(
                    "Verified Customer", "customer@aishield.com", passwordEncoder.encode("customer123"), "CUSTOMER", 
                    "+919800000002", "Mumbai, India", "Active cardholder with verified protection"
            ));
            cardRepository.save(new CardEntity(demoCustomer, "**** **** **** 5521", "5521", "VISA", "ACTIVE", 120000.0));
        }

        if (userRepository.findByEmail("pradeep@example.com").isPresent() && userRepository.count() > 3) {
            return; // Data already seeded
        }

        // 1. Seed Users (D1)
        UserEntity admin = userRepository.findByEmail("pradeep@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Pradeep", "pradeep@example.com", passwordEncoder.encode("password123"), "ADMIN", 
                "+919800000001", "Hyderabad, India", "Monitoring fraud activity across all transactions and tuning AI detection rules for the team."
        )));

        UserEntity rahul = userRepository.findByEmail("rahul@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Rahul Sharma", "rahul@example.com", passwordEncoder.encode("password123"), "CUSTOMER", 
                "+919800000002", "Hyderabad", "Active cardholder"
        )));

        UserEntity priya = userRepository.findByEmail("priya@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Priya Singh", "priya@example.com", passwordEncoder.encode("password123"), "CUSTOMER", 
                "+919800000003", "Bangalore", "Active cardholder"
        )));

        UserEntity arjun = userRepository.findByEmail("arjun@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Arjun Kumar", "arjun@example.com", passwordEncoder.encode("password123"), "CUSTOMER", 
                "+91 98333 44556", "Mumbai", "Active cardholder"
        )));

        UserEntity neha = userRepository.findByEmail("neha@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Neha Patel", "neha@example.com", passwordEncoder.encode("password123"), "CUSTOMER", 
                "+91 98444 55667", "Chennai", "Active cardholder"
        )));

        UserEntity vikram = userRepository.findByEmail("vikram@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Vikram Rao", "vikram@example.com", passwordEncoder.encode("password123"), "CUSTOMER", 
                "+91 98555 66778", "Delhi", "Active cardholder"
        )));

        UserEntity santu = userRepository.findByEmail("santu@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Santu Vanjarapu", "santu@example.com", passwordEncoder.encode("password123"), "CUSTOMER", 
                "+91 98666 77889", "Pune", "Active cardholder"
        )));

        UserEntity sneha = userRepository.findByEmail("sneha@example.com").orElseGet(() -> userRepository.save(new UserEntity(
                "Sneha Reddy", "sneha@example.com", passwordEncoder.encode("password123"), "CUSTOMER", 
                "+91 98777 88990", "Chennai", "Active cardholder"
        )));

        // 2. Seed Cards (D1)
        CardEntity cardRahul = cardRepository.save(new CardEntity(rahul, "**** **** **** 4721", "4721", "VISA", "ACTIVE", 150000.0));
        CardEntity cardPriya = cardRepository.save(new CardEntity(priya, "**** **** **** 9931", "9931", "MASTERCARD", "ACTIVE", 100000.0));
        CardEntity cardArjun = cardRepository.save(new CardEntity(arjun, "**** **** **** 6510", "6510", "VISA", "BLOCKED", 200000.0));
        CardEntity cardNeha = cardRepository.save(new CardEntity(neha, "**** **** **** 1276", "1276", "RUPAY", "ACTIVE", 75000.0));
        CardEntity cardVikram = cardRepository.save(new CardEntity(vikram, "**** **** **** 8401", "8401", "VISA", "ACTIVE", 120000.0));
        CardEntity cardSantu = cardRepository.save(new CardEntity(santu, "**** **** **** 7328", "7328", "MASTERCARD", "BLOCKED", 250000.0));
        CardEntity cardSneha = cardRepository.save(new CardEntity(sneha, "**** **** **** 6421", "6421", "VISA", "ACTIVE", 80000.0));

        // 3. Seed Fraud Rules (D3)
        ruleRepository.saveAll(List.of(
                new FraudRuleEntity("RULE_HIGH_AMOUNT", "Critical Transaction Amount Spike", "Flags transactions exceeding ₹1,00,000 or 3.5x customer baseline", "AMOUNT", 1.0, 100000.0, true),
                new FraudRuleEntity("RULE_VELOCITY_SPIKE", "High Transaction Velocity", "Flags > 3 consecutive transactions in less than 10 minutes", "VELOCITY", 1.0, 3.0, true),
                new FraudRuleEntity("RULE_IMPOSSIBLE_TRAVEL", "Impossible Travel Anomaly", "Flags cross-city/cross-country transactions under 60 minutes", "GEO", 1.2, 60.0, true),
                new FraudRuleEntity("RULE_UNKNOWN_DEVICE", "Unrecognized Hardware Footprint", "Flags transactions from unrecognized devices or suspicious IPs", "DEVICE", 0.8, 1.0, true),
                new FraudRuleEntity("RULE_RISKY_MERCHANT", "High Risk Merchant Category", "Flags Crypto Exchanges, Gambling, or Forex wire merchants", "MERCHANT", 1.0, 1.0, true)
        ));

        // 4. Seed Settings (D3/D5)
        settingsRepository.save(new SystemSettingsEntity(false, true, true, true, 80));

        // 5. Seed Historical Baseline Transactions (D2)
        LocalDateTime baseTime = LocalDateTime.now().minusHours(4);

        TransactionEntity tx1 = transactionRepository.save(new TransactionEntity(
                "#TX10001", rahul, cardRahul, "Rahul Sharma", "**** **** **** 4721", 25000.0, 
                "Amazon India", "RETAIL", "Hyderabad", "DEV-CHROME-WIN", "192.168.1.10", 
                baseTime.plusMinutes(15), "Approved", "Low", 12, "Legitimate transaction attributes verified", "APPROVE"
        ));

        TransactionEntity tx2 = transactionRepository.save(new TransactionEntity(
                "#TX10002", priya, cardPriya, "Priya Singh", "**** **** **** 9931", 82500.0, 
                "Croma Electronics", "ELECTRONICS", "Bangalore", "DEV-IPHONE-IOS", "192.168.1.15", 
                baseTime.plusMinutes(32), "Verification Required", "Medium", 72, "Elevated transaction amount (> ₹50,000); Off-hours purchase", "FLAG"
        ));

        TransactionEntity tx3 = transactionRepository.save(new TransactionEntity(
                "#TX10003", arjun, cardArjun, "Arjun Kumar", "**** **** **** 6510", 142000.0, 
                "Global Crypto Exchange", "CRYPTO", "Mumbai", "DEV-UNKNOWN-TOR", "185.220.101.5", 
                baseTime.plusMinutes(48), "CUSTOMER_REPORTED_FRAUD", "High", 98, "High-risk merchant category: CRYPTO; Impossible travel detected; Customer confirmed unauthorized fraud", "DECLINE"
        ));

        TransactionEntity tx4 = transactionRepository.save(new TransactionEntity(
                "#TX10004", neha, cardNeha, "Neha Patel", "**** **** **** 1276", 15800.0, 
                "Flipkart Retail", "RETAIL", "Chennai", "DEV-ANDROID-APP", "192.168.1.25", 
                baseTime.plusMinutes(70), "Approved", "Low", 8, "Legitimate transaction attributes verified", "APPROVE"
        ));

        TransactionEntity tx5 = transactionRepository.save(new TransactionEntity(
                "#TX10005", vikram, cardVikram, "Vikram Rao", "**** **** **** 8401", 65000.0, 
                "MakeMyTrip Flights", "TRAVEL", "Delhi", "DEV-SAFARI-MAC", "192.168.1.30", 
                baseTime.plusMinutes(95), "OWNER_VERIFIED", "Medium", 66, "Elevated amount above average baseline; Travel category; Step-Up Authenticated by Card Owner (WEBAUTHN_PASSKEY)", "APPROVE"
        ));

        TransactionEntity tx6 = transactionRepository.save(new TransactionEntity(
                "#TX10006", santu, cardSantu, "Santu Vanjarapu", "**** **** **** 7328", 218000.0, 
                "Luxury Watch Boutique", "JEWELRY", "Pune", "DEV-UNKNOWN-LINUX", "45.154.255.88", 
                baseTime.plusMinutes(114), "Blocked", "High", 96, "Critical amount threshold exceeded (> ₹2,00,000); Rapid velocity spikes", "DECLINE"
        ));

        TransactionEntity tx7 = transactionRepository.save(new TransactionEntity(
                "#TX10007", sneha, cardSneha, "Sneha Reddy", "**** **** **** 6421", 18000.0, 
                "Reliance Digital", "ELECTRONICS", "Chennai", "DEV-FIREFOX-WIN", "192.168.1.40", 
                baseTime.plusMinutes(122), "Approved", "Low", 21, "Legitimate transaction attributes verified", "APPROVE"
        ));

        // 6. Seed Fraud Alerts (D4)
        alertRepository.saveAll(List.of(
                new FraudAlertEntity("FA1023", tx3, "#TX10003", "Rahul Sharma", 120000.0, 98, "Hyderabad", "Blocked", "🚨 CUSTOMER CONFIRMED UNAUTHORIZED FRAUD: Card frozen immediately."),
                new FraudAlertEntity("FA1024", tx2, "#TX10002", "Priya Singh", 65000.0, 72, "Bangalore", "Reviewing", "High transaction velocity: 4 swipes in under 8 minutes across separate terminals [Step-Up Verification Pending]"),
                new FraudAlertEntity("FA1025", tx6, "#TX10006", "Arjun Kumar", 205000.0, 99, "Mumbai", "Blocked", "Unusual high-value withdrawal at high-risk jewelry merchant in off-hours"),
                new FraudAlertEntity("FA1026", tx7, "#TX10007", "Sneha Reddy", 18000.0, 21, "Chennai", "Resolved", "Cardholder step-up WebAuthn identity verified successfully via cardholder prompt")
        ));

        // 7. Seed Step-Up Verification Records
        TransactionVerificationEntity verifPriya = new TransactionVerificationEntity(
                "VR-89102-X",
                tx2,
                tx2.getTransactionRef(),
                priya,
                "priya@example.com",
                "Priya Singh",
                priya.getPhone() != null ? priya.getPhone() : "+91 98222 33445",
                "**** **** **** 9931",
                82500.0,
                "Croma Electronics",
                "Bangalore",
                "Medium",
                72,
                "PENDING",
                LocalDateTime.now().plusMinutes(15),
                "CHALLENGE-TOKEN-PRIYA-98214"
        );
        verifPriya.setSmsStatus("DELIVERED");
        verifPriya.setSmsContent("[AI SHIELD BANK ALERT] Suspicious charge of ₹82,500 at Croma Electronics flagged on Card ending 9931. Authorize: http://localhost:4200/verify/VR-89102-X");
        verifPriya.setAuditNotes("Verification request pending card owner step-up authorization; Real SMS sent to +91 98222 33445.");

        TransactionVerificationEntity verifVikram = new TransactionVerificationEntity(
                "VR-44120-K",
                tx5,
                tx5.getTransactionRef(),
                vikram,
                "vikram@example.com",
                "Vikram Rao",
                vikram.getPhone() != null ? vikram.getPhone() : "+91 98555 66778",
                "**** **** **** 8401",
                65000.0,
                "MakeMyTrip Flights",
                "Delhi",
                "Medium",
                66,
                "OWNER_CONFIRMED",
                LocalDateTime.now().minusMinutes(30),
                "CHALLENGE-TOKEN-VIKRAM-1123"
        );
        verifVikram.setSmsStatus("DELIVERED");
        verifVikram.setVerifiedAt(LocalDateTime.now().minusMinutes(28));
        verifVikram.setVerificationMethod("WEBAUTHN_PASSKEY");
        verifVikram.setSecurityMetadata("Method: WEBAUTHN_PASSKEY; VerifiedTimestamp: " + LocalDateTime.now().minusMinutes(28));
        verifVikram.setAuditNotes("Verified via platform biometric / passkey authentication.");

        verificationRepository.saveAll(List.of(verifPriya, verifVikram));

        // 7. Seed Initial Travel Notices (Travel Mode)
        TravelNoticeEntity travelPriya = new TravelNoticeEntity(
                priya,
                priya.getEmail(),
                "**** **** **** 8821",
                "United Arab Emirates",
                "Dubai",
                java.time.LocalDate.now().minusDays(1),
                java.time.LocalDate.now().plusDays(7)
        );

        TravelNoticeEntity travelVikram = new TravelNoticeEntity(
                vikram,
                vikram.getEmail(),
                "**** **** **** 8401",
                "United Kingdom",
                "London",
                java.time.LocalDate.now().plusDays(10),
                java.time.LocalDate.now().plusDays(20)
        );

        travelNoticeRepository.saveAll(List.of(travelPriya, travelVikram));
    }
}
