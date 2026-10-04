package com.aishield.fraud.service;

import com.aishield.fraud.dto.CustomerDtos;
import com.aishield.fraud.dto.TravelNoticeDtos;
import com.aishield.fraud.entity.CardEntity;
import com.aishield.fraud.entity.TransactionEntity;
import com.aishield.fraud.entity.TravelNoticeEntity;
import com.aishield.fraud.entity.UserEntity;
import com.aishield.fraud.exception.ResourceNotFoundException;
import com.aishield.fraud.repository.CardRepository;
import com.aishield.fraud.repository.TransactionRepository;
import com.aishield.fraud.repository.TravelNoticeRepository;
import com.aishield.fraud.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final TravelNoticeRepository travelNoticeRepository;
    private final com.aishield.fraud.repository.FraudAlertRepository fraudAlertRepository;
    private final AuditLoggingService auditLoggingService;
    private final TransactionService transactionService;

    public CustomerService(UserRepository userRepository, 
                           CardRepository cardRepository, 
                           TransactionRepository transactionRepository,
                           TravelNoticeRepository travelNoticeRepository,
                           com.aishield.fraud.repository.FraudAlertRepository fraudAlertRepository,
                           AuditLoggingService auditLoggingService,
                           TransactionService transactionService) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.travelNoticeRepository = travelNoticeRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.auditLoggingService = auditLoggingService;
        this.transactionService = transactionService;
    }

    public List<CustomerDtos.CustomerResponse> getAllCustomers() {
        List<UserEntity> users = userRepository.findAll();
        List<CustomerDtos.CustomerResponse> result = new ArrayList<>();

        for (UserEntity user : users) {
            List<CardEntity> cards = cardRepository.findByUserId(user.getId());
            String cardMasked = cards.isEmpty() ? "****4582" : cards.get(0).getCardNumberMasked();

            List<TransactionEntity> transactions = transactionRepository.findByUserIdOrderByTimestampDesc(user.getId());
            int txCount = transactions.size();

            int avgRisk = (int) Math.round(transactions.stream()
                    .mapToInt(TransactionEntity::getRiskScore)
                    .average()
                    .orElse(12.0));

            String status = avgRisk >= 80 ? "High" : (avgRisk >= 40 ? "Medium" : "Low");

            List<String> locations = transactions.stream()
                    .map(TransactionEntity::getLocation)
                    .distinct()
                    .filter(loc -> loc != null && !loc.isBlank())
                    .collect(Collectors.toList());

            if (locations.isEmpty() && user.getLocation() != null) {
                locations.add(user.getLocation());
            }

            result.add(new CustomerDtos.CustomerResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getMobileVerified(),
                    cardMasked,
                    txCount,
                    avgRisk,
                    status,
                    locations
            ));
        }

        return result;
    }

    public List<CustomerDtos.CardResponse> getCardsByUserEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElse(null);

        List<CardEntity> cards = (user != null) 
                ? cardRepository.findByUserId(user.getId()) 
                : cardRepository.findAll();

        return cards.stream().map(c -> new CustomerDtos.CardResponse(
                c.getId(),
                c.getUser() != null ? c.getUser().getName() : "Cardholder",
                c.getCardNumberMasked(),
                c.getCardType(),
                c.getStatus(),
                c.getDailyLimit(),
                c.getUser() != null ? c.getUser().getPhone() : "+91 98111 22334",
                c.getUser() != null ? c.getUser().getMobileVerified() : true
        )).collect(Collectors.toList());
    }

    @Transactional
    public CustomerDtos.CardResponse toggleCardLock(Long cardId) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + cardId));

        String newStatus = "ACTIVE".equalsIgnoreCase(card.getStatus()) ? "BLOCKED" : "ACTIVE";
        card.setStatus(newStatus);
        CardEntity saved = cardRepository.save(card);

        return new CustomerDtos.CardResponse(
                saved.getId(),
                saved.getUser() != null ? saved.getUser().getName() : "Cardholder",
                saved.getCardNumberMasked(),
                saved.getCardType(),
                saved.getStatus(),
                saved.getDailyLimit(),
                saved.getUser() != null ? saved.getUser().getPhone() : "+91 98111 22334",
                saved.getUser() != null ? saved.getUser().getMobileVerified() : true
        );
    }

    @Transactional
    public CustomerDtos.CardResponse updateDailyLimit(Long cardId, Double limit) {
        CardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with ID: " + cardId));

        if (limit != null && limit > 0) {
            card.setDailyLimit(limit);
        }
        CardEntity saved = cardRepository.save(card);

        return new CustomerDtos.CardResponse(
                saved.getId(),
                saved.getUser() != null ? saved.getUser().getName() : "Cardholder",
                saved.getCardNumberMasked(),
                saved.getCardType(),
                saved.getStatus(),
                saved.getDailyLimit(),
                saved.getUser() != null ? saved.getUser().getPhone() : "+91 98111 22334",
                saved.getUser() != null ? saved.getUser().getMobileVerified() : true
        );
    }

    public List<com.aishield.fraud.dto.TransactionDtos.TransactionResponse> getTransactionsByUserEmail(String email) {
        UserEntity user = (email != null && !email.isBlank())
                ? userRepository.findByEmail(email).orElse(null)
                : null;

        List<TransactionEntity> list;
        if (user != null) {
            list = transactionRepository.findByUserIdOrderByTimestampDesc(user.getId());
        } else if (email != null && !email.isBlank()) {
            list = transactionRepository.findByUserEmailOrCustomerName(email);
        } else {
            list = transactionRepository.findAll();
        }

        return list.stream()
                .map(transactionService::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerDtos.DisputeResponse disputeTransaction(String email, CustomerDtos.DisputeRequest request) {
        if (request == null || request.getTransactionRef() == null || request.getTransactionRef().isBlank()) {
            throw new com.aishield.fraud.exception.BadRequestException("Transaction reference is required for filing a dispute");
        }
        TransactionEntity tx = transactionRepository.findByTransactionRef(request.getTransactionRef())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + request.getTransactionRef()));

        tx.setStatus("CUSTOMER_REPORTED_FRAUD");
        String existingReasons = tx.getDecisionReasons() != null ? tx.getDecisionReasons() : "";
        tx.setDecisionReasons(existingReasons + "; Dispute filed by cardholder: " + (request.getReason() != null ? request.getReason() : "Unauthorized transaction"));
        transactionRepository.save(tx);

        // Update or create fraud alert
        String alertRef = "ALT-" + (System.currentTimeMillis() % 1000000);
        com.aishield.fraud.entity.FraudAlertEntity alert = fraudAlertRepository.findAll().stream()
                .filter(a -> a.getTransactionRef() != null && a.getTransactionRef().equalsIgnoreCase(tx.getTransactionRef()))
                .findFirst()
                .orElseGet(() -> new com.aishield.fraud.entity.FraudAlertEntity(
                        alertRef, tx, tx.getTransactionRef(), tx.getCustomerName(), tx.getAmount(),
                        tx.getRiskScore() != null ? tx.getRiskScore() : 90,
                        tx.getLocation(), "Reviewing", "Cardholder formal dispute"
                ));
        alert.setStatus("Reviewing");
        alert.setInvestigatorNotes("Cardholder opened formal dispute. Reason: " + request.getReason());
        fraudAlertRepository.save(alert);

        if (auditLoggingService != null) {
            String cardLast4 = tx.getCard() != null ? tx.getCard().getCardLast4() : "N/A";
            Long userId = tx.getUser() != null ? tx.getUser().getId() : null;
            auditLoggingService.logStepUpAction("TRANSACTION_DISPUTED", tx.getTransactionRef(), cardLast4, userId,
                    tx.getCustomerName(), "DISPUTED", "Cardholder filed dispute: " + request.getReason());
        }

        String disputeId = "DISP-" + (System.currentTimeMillis() % 1000000);
        return new CustomerDtos.DisputeResponse(
                disputeId,
                tx.getTransactionRef(),
                "UNDER_REVIEW",
                "Dispute successfully filed and submitted to Bank Fraud Operations.",
                java.time.LocalDateTime.now().toString()
        );
    }

    // ==========================================
    // TRAVEL NOTICE / TRAVEL MODE SERVICE METHODS
    // ==========================================
    @Transactional
    public TravelNoticeDtos.TravelNoticeResponse createTravelNotice(String userEmail, TravelNoticeDtos.CreateTravelNoticeRequest request) {
        String targetEmail = (userEmail != null && !userEmail.isBlank()) ? userEmail : "priya@example.com";
        UserEntity user = userRepository.findByEmail(targetEmail).orElse(null);

        String cardMasked = (request.getCardMasked() != null && !request.getCardMasked().isBlank())
                ? request.getCardMasked() : "**** **** **** 8821";

        TravelNoticeEntity notice = new TravelNoticeEntity(
                user,
                targetEmail,
                cardMasked,
                request.getDestinationCountry(),
                request.getDestinationCity(),
                request.getStartDate(),
                request.getEndDate()
        );

        TravelNoticeEntity saved = travelNoticeRepository.save(notice);
        return mapToTravelNoticeResponse(saved);
    }

    public List<TravelNoticeDtos.TravelNoticeResponse> getTravelNoticesByUserEmail(String userEmail) {
        String targetEmail = (userEmail != null && !userEmail.isBlank()) ? userEmail : "priya@example.com";
        return travelNoticeRepository.findByCustomerEmailOrderByCreatedAtDesc(targetEmail)
                .stream()
                .map(this::mapToTravelNoticeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean cancelTravelNotice(Long noticeId) {
        TravelNoticeEntity notice = travelNoticeRepository.findById(noticeId).orElse(null);
        if (notice != null) {
            notice.setStatus("CANCELLED");
            travelNoticeRepository.save(notice);
            return true;
        }
        return false;
    }

    private TravelNoticeDtos.TravelNoticeResponse mapToTravelNoticeResponse(TravelNoticeEntity entity) {
        TravelNoticeDtos.TravelNoticeResponse resp = new TravelNoticeDtos.TravelNoticeResponse();
        resp.setId(entity.getId());
        resp.setCustomerEmail(entity.getCustomerEmail());
        resp.setCardMasked(entity.getCardMasked());
        resp.setDestinationCountry(entity.getDestinationCountry());
        resp.setDestinationCity(entity.getDestinationCity());
        resp.setStartDate(entity.getStartDate());
        resp.setEndDate(entity.getEndDate());
        resp.setStatus(entity.getStatus());
        resp.setCreatedAt(entity.getCreatedAt());

        LocalDate now = LocalDate.now();
        boolean activeNow = "ACTIVE".equalsIgnoreCase(entity.getStatus())
                && !now.isBefore(entity.getStartDate())
                && !now.isAfter(entity.getEndDate());
        resp.setActiveNow(activeNow);

        return resp;
    }
}
