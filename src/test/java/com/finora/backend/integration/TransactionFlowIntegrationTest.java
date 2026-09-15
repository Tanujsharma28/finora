
package com.finora.backend.integration;

import com.finora.backend.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void fullFlow_registerLoginTransactAndFraudCheck() throws InterruptedException {
        String uniqueEmail = "test_" + UUID.randomUUID() + "@finora.com";

        // 1. Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Integration Test User");
        registerRequest.setEmail(uniqueEmail);
        registerRequest.setPassword("TestPass123");

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register", registerRequest, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = registerResponse.getBody().getToken();
        String userId = registerResponse.getBody().getUserId();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // 2. Create account
        CreateAccountRequest accountRequest = new CreateAccountRequest();
        accountRequest.setUserId(userId);
        accountRequest.setAccountType(com.finora.backend.domain.AccountType.SAVINGS);

        HttpEntity<CreateAccountRequest> accountEntity = new HttpEntity<>(accountRequest, headers);
        ResponseEntity<AccountResponse> accountResponse = restTemplate.postForEntity(
                baseUrl() + "/api/accounts", accountEntity, AccountResponse.class);

        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accountId = accountResponse.getBody().getId();

        // 3. Create a normal transaction — should clear
        CreateTransactionRequest normalTxn = new CreateTransactionRequest();
        normalTxn.setAccountId(accountId);
        normalTxn.setAmount(BigDecimal.valueOf(300));
        normalTxn.setTxnType(com.finora.backend.domain.TransactionType.CREDIT);
        normalTxn.setCategory("Test");

        HttpEntity<CreateTransactionRequest> normalEntity = new HttpEntity<>(normalTxn, headers);
        restTemplate.postForEntity(baseUrl() + "/api/transactions", normalEntity, TransactionResponse.class);

        // 4. Create a spike transaction — should be flagged
        CreateTransactionRequest spikeTxn = new CreateTransactionRequest();
        spikeTxn.setAccountId(accountId);
        spikeTxn.setAmount(BigDecimal.valueOf(95000));
        spikeTxn.setTxnType(com.finora.backend.domain.TransactionType.DEBIT);
        spikeTxn.setCategory("Test");

        HttpEntity<CreateTransactionRequest> spikeEntity = new HttpEntity<>(spikeTxn, headers);
        restTemplate.postForEntity(baseUrl() + "/api/transactions", spikeEntity, TransactionResponse.class);

        // 5. Wait for Kafka consumer to process async fraud check
        Thread.sleep(3000);

        // 6. Fetch transactions and verify statuses
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<TransactionResponse[]> txnListResponse = restTemplate.exchange(
                baseUrl() + "/api/transactions?accountId=" + accountId,
                HttpMethod.GET, getEntity, TransactionResponse[].class);

        assertThat(txnListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TransactionResponse[] transactions = txnListResponse.getBody();
        assertThat(transactions).hasSize(2);

        boolean hasFlagged = false;
        boolean hasCompleted = false;
        for (TransactionResponse txn : transactions) {
            if (txn.getStatus() == com.finora.backend.domain.TransactionStatus.FLAGGED) hasFlagged = true;
            if (txn.getStatus() == com.finora.backend.domain.TransactionStatus.COMPLETED) hasCompleted = true;
        }

        assertThat(hasFlagged).isTrue();
        assertThat(hasCompleted).isTrue();
    }
}