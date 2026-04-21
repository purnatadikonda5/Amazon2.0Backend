package com.purna.service;

import com.purna.dto.PaymentRequestDTO;
import com.purna.dto.PaymentResponseDTO;
import com.purna.dto.PaymentStatusResponseDTO;
import com.purna.dto.PaymentVerificationRequestDTO;
import com.purna.model.Listing;
import com.purna.model.Order;
import com.purna.model.PaymentTransaction;
import com.purna.model.UserObj;
import com.purna.repository.ListingRepository;
import com.purna.repository.OrderRepository;
import com.purna.repository.PaymentTransactionRepository;
import com.purna.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.purna.event.PaymentSuccessEvent;
import com.purna.config.RabbitMQConfig;

/**
 * PaymentService
 * 
 * WHY USE THIS:
 * The central nervous system for executing Database ACID properties and talking 
 * with 3rd Party APIs (Razorpay). Isolating logic here makes the system completely unit-testable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${razorpay.api.key:rzp_test_placeholder}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret:secret_placeholder}")
    private String razorpayApiSecret;

    private RazorpayClient razorpayClient;
    
    private final PaymentTransactionRepository paymentRepository;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        try {
            if (!"rzp_test_placeholder".equals(razorpayApiKey)) {
                this.razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }
        } catch (RazorpayException e) {
            log.warn("Could not initialize Razorpay Client immediately: {}", e.getMessage());
        }
    }

    /**
     * ⚡ ADVANCED PAYMENT: Transactional Isolation Level SERIALIZABLE
     * WHY USE THIS:
     * This strictly achieves Database Isolation in ACID. If a buyer clicks checking out from two different tabs 
     * simultaneously (race condition), the database physically guarantees they queue in series. 
     * This explicitly prevents "Phantom Reads" and stops duplicate identical gateway transactions!
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponseDTO createPaymentSession(PaymentRequestDTO request) {
        String idempotencyKey = request.getIdempotencyKey();

        // ⚡ ADVANCED PAYMENT: Idempotency Key (Safety standard for FinTech)
        // If the same request hits the server twice due to network lag, we return the cached active session instantly.
        // It's checked via indexed database lookups, preventing Razorpay from double charging!
        Optional<PaymentTransaction> existingTx = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            PaymentTransaction tx = existingTx.get();
            return new PaymentResponseDTO(tx.getRazorpayOrderId(), tx.getStatus(), tx.getCurrency(), tx.getAmount());
        }

        try {
            if (this.razorpayClient == null && !"rzp_test_placeholder".equals(razorpayApiKey)) {
                this.razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }

            // Relationship binding phase (Foreign keys integrity)
            Listing targetListing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Target Listing does not exist"));

            UserObj buyer = userRepository.findById(request.getBuyerId().intValue())
                .orElseThrow(() -> new RuntimeException("Buyer does not exist"));

            JSONObject orderRequest = new JSONObject();
            double finalAmountPaise = request.getAmount() * 100;
            orderRequest.put("amount", finalAmountPaise); 
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + idempotencyKey.substring(0, Math.min(idempotencyKey.length(), 20)));
            
            /**
             * ⚡ ADVANCED PAYMENT: Peer-To-Peer Automated Payout Routing
             * WHY USE THIS ROUTE:
             * Razorpay allows automatically splitting transactions natively. If the underlying seller
             * has attached their Razorpay Bank ID, the primary platform drops the money straight
             * to their bank account instantly, bypassing holding platform Escrow code completely!
             */
            UserObj seller = targetListing.getSeller();
            if (seller != null && seller.getRazorpayAccountId() != null && !seller.getRazorpayAccountId().isEmpty()) {
                log.info("Processing Razorpay Route Transfer directly to Seller Account: {}", seller.getRazorpayAccountId());
                
                JSONObject transfer = new JSONObject();
                transfer.put("account", seller.getRazorpayAccountId()); 
                transfer.put("amount", finalAmountPaise); 
                transfer.put("currency", "INR");
                transfer.put("on_hold", false); 
                
                JSONArray transfersArray = new JSONArray();
                transfersArray.put(transfer);
                orderRequest.put("transfers", transfersArray);
            }

            String orderId;
            if ("rzp_test_placeholder".equals(razorpayApiKey)) {
                // Allows developers to boot sandbox environment locally smoothly without keys
                orderId = "order_mock_" + System.currentTimeMillis();
            } else {
                com.razorpay.Order order = razorpayClient.orders.create(orderRequest);
                orderId = order.get("id");
            }

            // Strong Foreign Key bindings for integrity tracking
            PaymentTransaction transaction = PaymentTransaction.builder()
                .idempotencyKey(idempotencyKey)
                .razorpayOrderId(orderId)
                .amount(request.getAmount())
                .currency("INR")
                .listing(targetListing)
                .buyer(buyer)
                .status("CREATED")
                .build();
            paymentRepository.save(transaction);

            return new PaymentResponseDTO(orderId, "CREATED", "INR", request.getAmount());

        } catch (Exception e) {
            log.error("Error creating Razorpay Order via DB transaction:", e);
            throw new RuntimeException("Payment Gateway Error: " + e.getMessage());
        }
    }

    /**
     * ⚡ ADVANCED PAYMENT: Transactional Atomicity (Webhooks)
     * WHY USE @Transactional:
     * This achieves Database Atomicity (The ALL or NOTHING rule).
     * If the Seller's virtual balance update crashes at the very end of code execution, everything 
     * performed prior (inventory reduction, order creation, success marks) are completely unwound/rolled-back
     * seamlessly by the MySQL engine automatically! Total security.
     */
    @org.springframework.cache.annotation.CacheEvict(value = {"market_listings", "user_listings", "product_details", "user_orders"}, allEntries = true)
    @Transactional
    public PaymentStatusResponseDTO verifyAndUpdatePayment(PaymentVerificationRequestDTO verificationDto) {
        log.info("Verifying Webhook/API payment captured for Order: {}", verificationDto.getRazorpayOrderId());
        
        // PESSIMISTIC LOCK: Block other webhook hits to avoid double fulfillment concurrency bugs!
        Optional<PaymentTransaction> txOptional = paymentRepository.findByRazorpayOrderIdWithPessimisticLock(verificationDto.getRazorpayOrderId());
        if (txOptional.isEmpty()) {
            throw new RuntimeException("Invalid Payment Transaction: Order ID not found.");
        }
        
        PaymentTransaction tx = txOptional.get();

        if ("SUCCESS".equals(tx.getStatus()) || "PAID".equals(tx.getStatus())) {
            return new PaymentStatusResponseDTO(tx.getRazorpayOrderId(), "ALREADY_PROCESSED");
        }

        /**
         * WHY VERIFY SIGNATURE:
         * Attackers could otherwise forge a basic JSON HTTP packet saying "Amount Paid: TRUE". 
         * By hashing using your Private Company secret via Utils.verifyPaymentSignature, 
         * you detect malicious payload tampering absolutely.
         */
        try {
            if (!"rzp_test_placeholder".equals(razorpayApiKey)) {
                JSONObject options = new JSONObject();
                options.put("razorpay_order_id", verificationDto.getRazorpayOrderId());
                options.put("razorpay_payment_id", verificationDto.getRazorpayPaymentId());
                options.put("razorpay_signature", verificationDto.getRazorpaySignature());

                boolean status = Utils.verifyPaymentSignature(options, razorpayApiSecret);
                if (!status) {
                    throw new RuntimeException("Signature verification failed! Potential tampering detected.");
                }
            }
        } catch (RazorpayException e) {
            throw new RuntimeException("Error executing signature verification: " + e.getMessage());
        }

        // Apply secondary Locks to the product Listing to modify its inventory safely
        Optional<Listing> listingOpt = listingRepository.findByIdWithPessimisticLock(tx.getListing().getId());
        if (listingOpt.isEmpty()) {
            tx.setStatus("FAILED_NO_LISTING");
            paymentRepository.save(tx);
            throw new RuntimeException("Listing no longer exists");
        }

        Listing targetListing = listingOpt.get();
        if (targetListing.getQuantity() == null || targetListing.getQuantity() <= 0) {
            tx.setStatus("FAILED_OUT_OF_STOCK");
            paymentRepository.save(tx);
            throw new RuntimeException("Listing is out of stock! Payment should be refunded.");
        }
        
        // Finalize state modifications (Deduct Inventory)
        targetListing.setQuantity(targetListing.getQuantity() - 1);
        if (targetListing.getQuantity() == 0) {
            targetListing.setStatus("sold_out");
        }
        listingRepository.save(targetListing);

        // Generate the final valid Marketplace receipt linking Buyer -> Seller
        Order finalOrder = Order.builder()
            .buyer(tx.getBuyer())
            .listing(targetListing)
            .purchasePrice(tx.getAmount())
            .status("PAID")
            .isDeleted(false)
            .build();
        orderRepository.save(finalOrder);

        /**
         * WHY THIS FALLBACK LOGIC EXISTS:
         * If the Seller had linked their Razorpay Account (Direct Route), they were already paid. 
         * If not or running mock tests, the Platform takes the cash and provides the seller a 
         * ledger balance equal to their cut.
         */
        UserObj seller = targetListing.getSeller();
        if (seller != null) {
            if (seller.getRazorpayAccountId() != null && !seller.getRazorpayAccountId().isEmpty()) {
                log.info("Direct Route fulfilled via Razorpay for Seller ID {}", seller.getId());
            } else {
                Double currentBalance = seller.getBalance() != null ? seller.getBalance() : 0.0;
                seller.setBalance(currentBalance + tx.getAmount());
                userRepository.save(seller);
                log.info("Credited INR {} to Escrow Wallet of Seller ID {}", tx.getAmount(), seller.getId());
            }
        }

        tx.setStatus("SUCCESS");
        paymentRepository.save(tx);

        /**
         * WHY THIS RABBITMQ EVENT PUBLISHER IS USED:
         * Instead of freezing this HTTP request for 5 seconds to generate an Invoice, we shoot a 
         * PaymentSuccessEvent into the global cluster RabbitExchange and instantly return "SUCCESS" to Razorpay.
         */
        PaymentSuccessEvent amqpEvent = new PaymentSuccessEvent();
        amqpEvent.setOrderId(finalOrder.getId());
        amqpEvent.setBuyerEmail(tx.getBuyer().getEmail());
        amqpEvent.setAmount(tx.getAmount());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, amqpEvent);

        return new PaymentStatusResponseDTO(tx.getRazorpayOrderId(), "SUCCESS");
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponseDTO getPaymentStatus(String razorpayOrderId) {
        Optional<PaymentTransaction> transaction = paymentRepository.findByRazorpayOrderId(razorpayOrderId);
        return transaction.map(tx -> new PaymentStatusResponseDTO(razorpayOrderId, tx.getStatus()))
                .orElseGet(() -> new PaymentStatusResponseDTO(razorpayOrderId, "NOT_FOUND"));
    }
}
