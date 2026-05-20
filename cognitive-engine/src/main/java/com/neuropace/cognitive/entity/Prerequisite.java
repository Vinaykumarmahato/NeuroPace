package com.neuropace.cognitive.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prerequisites", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"subject_id", "requires_subject_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requires_subject_id", nullable = false)
    private Subject requiresSubject;

    @Column(nullable = false)
    private Double weight;
}
