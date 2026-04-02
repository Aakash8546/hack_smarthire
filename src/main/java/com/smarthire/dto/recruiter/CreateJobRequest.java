package com.smarthire.dto.recruiter;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateJobRequest {

    @NotBlank
    private String title;

    private String company;

    @NotBlank
    private String location;

    @NotBlank
    @Size(max = 5000)
    private String description;

    @NotNull
    @Min(0)
    private Integer experience;

    @NotBlank
    private String jobPackage;

    @NotEmpty
    private List<String> requiredSkills;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public String getJobPackage() {
        return jobPackage;
    }

    public void setJobPackage(String jobPackage) {
        this.jobPackage = jobPackage;
    }

    public void setPackage(String jobPackage) {
        this.jobPackage = jobPackage;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }
}
