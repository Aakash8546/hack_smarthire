package com.smarthire.entity;

import com.smarthire.entity.enums.InterviewType;
import com.smarthire.entity.enums.MockInterviewSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "interviews")
public class Interview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Column(columnDefinition = "TEXT")
    private String technicalAnalysis;

    @Column(columnDefinition = "TEXT")
    private String behavioralAnalysis;

    private String meetingRoomId;

    private String meetingUrl;

    @Column(columnDefinition = "TEXT")
    private String cheatingDetectionResult;

    @Column(columnDefinition = "TEXT")
    private String mockInterviewQuestionsJson;

    @Column(columnDefinition = "TEXT")
    private String mockInterviewAnswersJson;

    private Integer currentQuestionIndex;

    @Column(nullable = false)
    private boolean completed;

    @Column(columnDefinition = "TEXT")
    private String mockInterviewAnalysisJson;

    @Enumerated(EnumType.STRING)
    @Column
    private MockInterviewSessionStatus sessionStatus;
}
