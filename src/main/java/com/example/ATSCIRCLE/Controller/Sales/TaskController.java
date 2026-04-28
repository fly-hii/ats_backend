package com.example.ATSCIRCLE.Controller.Sales;

import com.example.ATSCIRCLE.Models.Sales.Task;
import com.example.ATSCIRCLE.Service.Sales.TaskService;
import com.example.ATSCIRCLE.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private JwtService jwtService;

    // ✅ EXACT SAME AS CompanyController
  private Map<String, String> extractUserContext(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        String organizationId = jwtService.extractOrganizationId(token);
        String type = jwtService.extractType(token);  // This will be "EMPLOYEE" or organization type
        String userId = jwtService.extractId(token);
        
        return Map.of(
            "organizationId", organizationId,
            "type", type,
            "userId", userId
        );
    }
    

    // ✅ Create Task
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> createTask(@RequestBody Task task,
                                        Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            Task createdTask = taskService.createTask(
                task, 
                context.get("organizationId"), 
                context.get("userId")
            );
            return ResponseEntity.ok(createdTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Create Error",
                "message", e.getMessage(),
                "status", 400
            ));
        }
    }

    // ✅ Get all tasks
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTasks(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        System.out.println(context.get("organizationId"));
        return ResponseEntity.ok(taskService.getAllTasks(context.get("organizationId")));
    }

    // ✅ Get my tasks
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getMyTasks(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getMyTasks(
            context.get("userId"), context.get("organizationId")));
    }
    // ✅ Update Task
    @PatchMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> updateTask(@PathVariable String id,
                                    @RequestBody Task updatedTask,
                                    Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);

        Optional<Task> task = taskService.updateTask(
                id,
                updatedTask,
                context.get("organizationId"),
                context.get("userId"),
                context.get("type")
        );

        return task.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

    } catch (RuntimeException e) {
        return ResponseEntity.status(403).body(Map.of(
                "error", "Update Error",
                "message", e.getMessage(),
                "status", 403
        ));
    }
}

    // ✅ Delete Task (ADMIN ONLY)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTask(@PathVariable String id,
                                        Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            taskService.deleteTask(
                id, 
                context.get("organizationId"), 
                context.get("userId"),
                context.get("type")
            );
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Delete Error",
                "message", e.getMessage(),
                "status", 403
            ));
        }
    }

    // 🔥 Bulk Delete
    @DeleteMapping("/bulkdelete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkDeleteTasks(@RequestBody List<String> taskIds,
                                              Authentication authentication) {
        try {
            Map<String, String> context = extractUserContext(authentication);
            
            int deletedCount = taskService.bulkDeleteTasks(
                taskIds, 
                context.get("organizationId"), 
                context.get("userId"),
                context.get("type")
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Tasks deleted successfully",
                "deletedCount", deletedCount,
                "taskIds", taskIds
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Bulk Delete Error",
                "message", e.getMessage(),
                "status", 403
            ));
        }
    }

    // 🔥 Bulk Update
    @SuppressWarnings("unchecked")
    @PutMapping("/bulk-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkUpdateField(@RequestBody Map<String, Object> payload,
                                              Authentication authentication) {
        try {
            List<String> ids = (List<String>) payload.get("ids");
            String fieldName = (String) payload.get("fieldName");
            Object fieldValue = payload.get("fieldValue");
            
            if (ids == null || ids.isEmpty() || fieldName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Request",
                    "message", "Provide ids, fieldName, and fieldValue."
                ));
            }
            
            Map<String, String> context = extractUserContext(authentication);

            taskService.bulkUpdateField(ids, fieldName, fieldValue, 
                context.get("organizationId"), context.get("userId"), context.get("type"));

            return ResponseEntity.ok(Map.of(
                "message", "Updated field '" + fieldName + "' for " + ids.size() + " tasks."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Bulk Update Error",
                "message", e.getMessage(),
                "status", 400
            ));
        }
    }

    // 🔥 Bulk Assign
    @SuppressWarnings("unchecked")
    @PutMapping("/bulk-assign")
   @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> bulkAssignTasks(@RequestBody Map<String, Object> payload,
                                              Authentication authentication) {
        try {
            List<String> ids = (List<String>) payload.get("ids");
            String newAssignee = (String) payload.get("newAssignee");
            
            if (ids == null || ids.isEmpty() || newAssignee == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid Request",
                    "message", "Provide ids and newAssignee."
                ));
            }
            
            Map<String, String> context = extractUserContext(authentication);

            taskService.bulkAssign(ids, newAssignee,
                context.get("organizationId"), context.get("userId"), context.get("type"));

            return ResponseEntity.ok(Map.of(
                "message", ids.size() + " tasks assigned to " + newAssignee
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Bulk Assign Error",
                "message", e.getMessage(),
                "status", 403
            ));
        }
    }

    

    // ✅ Get tasks for specific user
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getUserTasks(@PathVariable String userId,
                                           Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getMyTasks(userId, context.get("organizationId")));
    }

    // ✅ Today tasks for current user
    @GetMapping("/today/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getMyTodayTasks(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getTodayTasks(
            context.get("userId"), context.get("organizationId")));
    }

    // ✅ Upcoming tasks for current user
    @GetMapping("/upcoming/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getMyUpcomingTasks(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getUpcomingTasks(
            context.get("userId"), context.get("organizationId")));
    }

    // ✅ Overdue tasks for current user
    @GetMapping("/overdue/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getMyOverdueTasks(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getOverdueTasks(
            context.get("userId"), context.get("organizationId")));
    }

    // ✅ Today tasks (all)
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getTodayTasksAll(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getTodayTasksAll(context.get("organizationId")));
    }

    // ✅ Upcoming tasks (all)
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getUpcomingTasksAll(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getUpcomingTasksAll(context.get("organizationId")));
    }

    // ✅ Overdue tasks (all)
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
    public ResponseEntity<?> getOverdueTasksAll(Authentication authentication) {
        Map<String, String> context = extractUserContext(authentication);
        return ResponseEntity.ok(taskService.getOverdueTasksAll(context.get("organizationId")));
    }

    // ✅ Get tasks by linked contact ID (ordered by due date)
@GetMapping("/by-contact/{contactId}")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> getTasksByContact(@PathVariable String contactId,
                                            Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);
        List<Task> tasks = taskService.getTasksByLinkedContact(
                contactId, context.get("organizationId"));
        return ResponseEntity.ok(tasks);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Fetch Error",
                "message", e.getMessage(),
                "status", 400
        ));
    }
}

// ✅ Get tasks by linked account/company ID (ordered by due date)
@GetMapping("/by-account/{accountId}")
@PreAuthorize("hasAnyRole('ADMIN', 'CRM')")
public ResponseEntity<?> getTasksByAccount(@PathVariable String accountId,
                                            Authentication authentication) {
    try {
        Map<String, String> context = extractUserContext(authentication);
        List<Task> tasks = taskService.getTasksByLinkedAccount(
                accountId, context.get("organizationId"));
        return ResponseEntity.ok(tasks);
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Fetch Error",
                "message", e.getMessage(),
                "status", 400
        ));
    }
}
}