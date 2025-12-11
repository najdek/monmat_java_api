package pl.monmat.manager.api;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
public class Order {
    @Getter
    @Id // klucz glowny
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sys_id")
    private Long sysId;

    @Column(name = "id", unique = true, nullable = false)
    private String orderNumber;

    @Getter
    @Setter
    private String email;

}
