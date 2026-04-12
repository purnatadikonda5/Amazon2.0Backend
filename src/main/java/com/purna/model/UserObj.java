package com.purna.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.Index;

/**
 * UserObj Database Entity
 *
 * WHY USE THIS:
 * The foundational model for both Buyers and Sellers. It utilizes @SQLDelete and @SQLRestriction 
 * to implement "Soft Deletion". In an advanced e-commerce system, you never physically run 'DELETE FROM'.
 * Instead, we toggle `is_deleted = true`. This preserves financial audit trails (ACID Durability) so 
 * Order receipts or Payment Transactions attached to this user don't suddenly throw NullPointerExceptions 
 * or violate Database Foreign Key constraints if the user decides to close their account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="AmazonProUser", indexes = {
    @Index(name = "idx_user_email", columnList = "email")
})
@SQLDelete(sql = "UPDATE amazon_pro_user SET is_deleted = true WHERE id=?")
@SQLRestriction("is_deleted=false")
public class UserObj {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	private String name;
	
	@Column(unique = true)
	private String email;
	
	private String password;
	
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Builder.Default
    private Double balance = 0.0;
    
    @Column(name = "razorpay_account_id")
    private String razorpayAccountId;

}
