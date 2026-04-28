// package com.example.ATSCIRCLE.Service.Sales;

// import com.example.ATSCIRCLE.Models.Sales.Task;
// import com.example.ATSCIRCLE.Repository.TaskRepository;
// import com.example.ATSCIRCLE.Service.EmailService;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.util.List;

// @Service
// public class ReminderScheduler {

//     @Autowired
//     private TaskRepository taskRepository;

//     @Autowired
//     private EmailService mailService;

//     @Autowired
//     private NotificationService notificationService;

//     @Scheduled(fixedRate = 60000) // every 1 min
//     public void checkReminders() {
//         LocalDateTime now = LocalDateTime.now();
//         List<Task> tasks = taskRepository.findAll();

//         for (Task task : tasks) {
//             LocalDateTime reminderTime = getReminderTime(task);

//             if (reminderTime != null &&
//                 reminderTime.isBefore(now.plusMinutes(1)) &&
//                 reminderTime.isAfter(now.minusMinutes(1))) {

//                 // Send Mail
//                 mailService.sendTaskReminder(
//                         task.getAssignedTo(),
//                         "⏰ Task Reminder: " + task.getSubject(),
//                         "Your task '" + task.getSubject() + "' is due at " + task.getDueDate()
//                 );

//                 // Send Notification
//                 notificationService.sendNotification(
//                         task.getAssignedTo(),
//                         "⏰ Task Reminder: " + task.getSubject()
//                 );
//             }
//         }
//     }

//     private LocalDateTime getReminderTime(Task task) {
//         if (task.getReminder() == null || task.getDueDate() == null) return null;

//         return switch (task.getReminder()) {
//             case THIRTY_MINUTES_BEFORE -> task.getDueDate().minusMinutes(30);
//             case ONE_HOUR_BEFORE -> task.getDueDate().minusHours(1);
//             case ONE_DAY_BEFORE -> task.getDueDate().minusDays(1);
//             default -> null;
//         };
//     }
// }
package com.example.ATSCIRCLE.Service.Sales;

import com.example.ATSCIRCLE.Models.Sales.Task;
import com.example.ATSCIRCLE.Models.UserManagement.Employee;
import com.example.ATSCIRCLE.Repository.TaskRepository;
import com.example.ATSCIRCLE.Repository.EmployeeRepository;
import com.example.ATSCIRCLE.Service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReminderScheduler {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployeeRepository employeeRepository; // ✅ Add this

    @Autowired
    private EmailService mailService;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedRate = 60000)
    public void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> tasks = taskRepository.findAll();

        for (Task task : tasks) {
            LocalDateTime reminderTime = getReminderTime(task);

            if (reminderTime != null &&
                reminderTime.isBefore(now.plusMinutes(1)) &&
                reminderTime.isAfter(now.minusMinutes(1))) {

                // ✅ Get employee email from assignedTo (employeeId)
                if (task.getAssignedTo() == null) continue;

                Optional<Employee> employeeOpt = employeeRepository.findById(task.getAssignedTo());
                if (employeeOpt.isEmpty()) continue; // Skip if employee not found

                String employeeEmail = employeeOpt.get().getEmail();

                // Send Mail using actual email
                mailService.sendTaskReminder(
                        employeeEmail, // ✅ email id use chestunnam
                        "⏰ Task Reminder: " + task.getSubject(),
                        "Your task '" + task.getSubject() + "' is due at " + task.getDueDate()
                );

                // Send Notification (employeeId use cheyochu - socket/push ki)
                notificationService.sendNotification(
                        task.getAssignedTo(), // employeeId fine here
                        "⏰ Task Reminder: " + task.getSubject()
                );
            }
        }
    }

    private LocalDateTime getReminderTime(Task task) {
        if (task.getReminder() == null || task.getDueDate() == null) return null;

        return switch (task.getReminder()) {
            case THIRTY_MINUTES_BEFORE -> task.getDueDate().minusMinutes(30);
            case ONE_HOUR_BEFORE -> task.getDueDate().minusHours(1);
            case ONE_DAY_BEFORE -> task.getDueDate().minusDays(1);
            default -> null;
        };
    }
}