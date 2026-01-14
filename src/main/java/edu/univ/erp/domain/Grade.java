package edu.univ.erp.domain;

public class Grade {
    private int gradeId;
    private int enrollmentId;
    private String component;
    private Double score;
    private String finalGrade;

    public Grade() {}

    public void setGradeId(int gradeId) { this.gradeId = gradeId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getFinalGrade() { return finalGrade; }
    public void setFinalGrade(String finalGrade) { this.finalGrade = finalGrade; }
}

