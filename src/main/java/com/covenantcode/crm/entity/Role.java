package com.covenantcode.crm.entity;


import com.covenantcode.crm.entity.enums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.*;


@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private RoleName name;
}
