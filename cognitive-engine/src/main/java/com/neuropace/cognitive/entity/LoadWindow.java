package com.neuropace.cognitive.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "load_windows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;

    @Column(name = "event_count", nullable = false)
    private Integer eventCount;

    @Column(name = "avg_response_time")
    private Double avgResponseTime;
}
