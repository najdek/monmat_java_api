package pl.monmat.manager.api;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import pl.monmat.manager.api.json.Address;
import pl.monmat.manager.api.json.InvoiceDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter @Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Address shippingAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private InvoiceDetails invoiceDetails;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String paidCurrency;

    @Column(columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String shippingCostCurrency;

    private UUID uuid;
    private String externalOrderId;
    private String email;
    private String phoneNumber;
    private String username;
    private Boolean is_guest;
    private BigDecimal totalPaidAmount;
    private BigDecimal shippingCost;
    private String status;
    private LocalDateTime boughtAt;
    private LocalDateTime paymentAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private Boolean needsInvoice;
    private String deliveryMethodId;
    private String deliveryMethodName;
    private String pickupPointId;
    private String trackingNumbers;
    private String customerComment;
    private String internalNotes;
    private Boolean isSmart;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
