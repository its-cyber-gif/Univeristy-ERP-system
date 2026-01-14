package edu.univ.erp.service;
import edu.univ.erp.access.AccessControl;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;
import edu.univ.erp.config.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;
import edu.univ.erp.data.GradeDAO;

public class StudentService {
    private SectionDAO sectionDAO = new SectionDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private CourseDAO courseDAO = new CourseDAO();
    private GradeDAO gradeDAO = new GradeDAO();
    private NotificationDAO notificationDAO = new NotificationDAO();

    public List<Map<String,Object>> getCourseCatalogForUI() {
        List<Map<String,Object>> out = new ArrayList<>();

        for (Section sec : sectionDAO.listAll()) {
            Course c = courseDAO.findById(sec.getCourseId());
            if (c == null) continue;

            Map<String,Object> row = new HashMap<>();
            row.put("section_id", sec.getSectionId());
            row.put("code", c.getCode());
            row.put("title", c.getTitle());
            row.put("credits", c.getCredits());
            row.put("capacity", sec.getCapacity());
            row.put("instructor_id", sec.getInstructorId());
            out.add(row);
        }

        return out;
    }

    public String registerForSection(int studentId, int sectionId) {

        if (AccessControl.isMaintenanceOn())
            return "Maintenance is ON — cannot register.";
        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT setting_value FROM system_settings WHERE setting_key='registration_deadline'"
             )) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String deadlineStr = rs.getString(1);
                System.out.println("DEADLINE READ = '" + deadlineStr + "'");

                if (deadlineStr != null && !deadlineStr.isBlank()) {
                    LocalDateTime deadline = LocalDateTime.parse(deadlineStr);
                    if (LocalDateTime.now().isAfter(deadline)) {
                        return "Registration deadline passed (Deadline was: " + deadlineStr + ")";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Section sec = sectionDAO.findById(sectionId);
        if (sec == null)
            return "Section not found.";

        int enrolled = sectionDAO.countEnrolled(sectionId);
        if (enrolled >= sec.getCapacity())
            return "Section full.";

        if (enrollmentDAO.exists(studentId, sectionId))
            return "Already enrolled in this section.";

        boolean ok = enrollmentDAO.create(studentId, sectionId);
        if (ok) {
            notificationDAO.add(studentId, "Registered for section " + sectionId);
            return "Registered successfully.";
        }
        else return "Failed to register.";

    }

    public String dropSection(int studentId, int sectionId) {

        if (AccessControl.isMaintenanceOn())
            return "Maintenance is ON — cannot drop.";

        try (Connection con = DBConfig.getErpDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT setting_value FROM system_settings WHERE setting_key='registration_deadline'"
             )) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String deadlineStr = rs.getString(1);

                if (deadlineStr != null && !deadlineStr.isBlank()) {
                    LocalDateTime deadline = LocalDateTime.parse(deadlineStr);

                    if (LocalDateTime.now().isAfter(deadline)) {
                        return "Drop not allowed after deadline (" + deadlineStr + ")";
                    }
                }
            }
        } catch (Exception ignored) {}

        boolean ok = enrollmentDAO.drop(studentId, sectionId);
        return ok ? "Dropped successfully." : "Drop failed (not enrolled).";
    }

    public List<Map<String,Object>> getNotifications(int studentId) {
        return notificationDAO.listUnread(studentId);
    }

    public List<Map<String, Object>> getMyRegistrations(int studentId) {
        List<Map<String, Object>> out = new ArrayList<>();

        List<Integer> sectionIds = enrollmentDAO.listSectionIdsForStudent(studentId);

        for (Integer sid : sectionIds) {
            Section sec = sectionDAO.findById(sid);
            if (sec == null) continue;

            Course c = courseDAO.findById(sec.getCourseId());
            if (c == null) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("section_id", sec.getSectionId());
            row.put("code", c.getCode());
            row.put("title", c.getTitle());
            row.put("credits", c.getCredits());
            row.put("capacity", sec.getCapacity());
            row.put("instructor_id", sec.getInstructorId());
            row.put("day_time", sec.getDayTime());
            row.put("room", sec.getRoom());

            out.add(row);
        }

        return out;
    }

    public List<Map<String, Object>> getGrades(int studentId) {
        return gradeDAO.getGrades(studentId);
    }
    public int getCompletedCredits(int studentId) {
        return gradeDAO.getTotalCreditsCompleted(studentId);
    }

}




