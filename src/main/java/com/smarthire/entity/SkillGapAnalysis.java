

package com.smarthire.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "skill_gap_analyses")
public class SkillGapAnalysis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "skill_gap_missing_skills", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "skill")
    private List<String> missingSkills = new ArrayList<>();

    @Lob
    @Column(nullable = false, length = 5000)
    private String roadmap;
}
