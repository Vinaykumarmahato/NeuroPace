package com.neuropace.cognitive.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "difficulty_level", nullable = false)
    private Integer difficultyLevel;

    @Column(name = "topic_area")
    private String topicArea;
}
