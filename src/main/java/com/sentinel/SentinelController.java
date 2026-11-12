package com.sentinel;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;

@RestController
@RequestMapping("/api")
class SentinelController {
 private final JdbcTemplate db; private final PasswordEncoder encoder;
 @Value("${sentinel.storage-dir:./storage/evidence}") String storageDir;
 @Value("${sentinel.monitor.enabled:true}") boolean monitorEnabled;
 SentinelController(JdbcTemplate db,PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
 
private String uid(HttpSession s) {
    Object id = s.getAttribute("userId");
    if (id == null) {
        throw new org.springframework.web.server.ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Please sign in again."
        );
    }
    return String.valueOf(id);
}

private void event(
        String evidenceId, String actor, String action,
        String previous, String next, String details) {

    String eventId = UUID.randomUUID().toString();

    db.update(
        "INSERT INTO custody_events " +
        "(event_id, evidence_id, actor_user_id, from_user_id, to_user_id, event_type, notes) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
        eventId, evidenceId, actor, previous, next, action, details
    );
}

 private String role(HttpSession s){return String.valueOf(s.getAttribute("role"));}
 private void admin(HttpSession s){if(!"ADMIN".equals(role(s)))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator role required.");}
 
 
private void alert(String evidenceId, String type, String message) {
    String fullMessage = type + ": " + message;
    Integer existing = db.queryForObject(
        "SELECT COUNT(*) FROM alerts " +
        "WHERE evidence_id = ? AND resolved = FALSE AND message = ?",
        Integer.class, evidenceId, fullMessage
    );
    if (existing == null || existing == 0) {
        db.update(
            "INSERT INTO alerts (alert_id, evidence_id, message, severity, resolved, created_at) " +
            "VALUES (?, ?, ?, ?, FALSE, NOW())",
            UUID.randomUUID().toString(), evidenceId, fullMessage, "CRITICAL"
        );
    }
}


 @PostMapping("/auth/login") Map<String,Object> login(@RequestBody Map<String,String>b,HttpServletRequest req){
  var rows=db.queryForList("SELECT user_id,full_name,username,password_hash,role,active FROM users WHERE username=?",b.get("username"));
  if(rows.isEmpty()||!Boolean.TRUE.equals(rows.get(0).get("active"))||!encoder.matches(b.getOrDefault("password",""),String.valueOf(rows.get(0).get("password_hash"))))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid login or inactive account.");
  HttpSession old=req.getSession(false);if(old!=null)old.invalidate();HttpSession s=req.getSession(true);var u=rows.get(0);
  s.setAttribute("userId",u.get("user_id"));s.setAttribute("name",u.get("full_name"));s.setAttribute("username",u.get("username"));s.setAttribute("role",u.get("role"));
  return Map.of("id",u.get("user_id"),"name",u.get("full_name"),"username",u.get("username"),"role",u.get("role"));
 }
 @GetMapping("/auth/me") ResponseEntity<?> me(HttpSession s){if(s.getAttribute("userId")==null)return ResponseEntity.status(401).body(Map.of("error","Not signed in"));return ResponseEntity.ok(Map.of("id",s.getAttribute("userId"),"name",s.getAttribute("name"),"username",s.getAttribute("username"),"role",s.getAttribute("role")));}
 @PostMapping("/auth/logout") Map<String,String> logout(HttpSession s){s.invalidate();return Map.of("message","Signed out");}



 @GetMapping("/dashboard/stats")
 Map<String, Object> stats() {
    return Map.of(
        "totalEvidence", count("SELECT COUNT(*) FROM evidence"),
        "verifiedEvidence", count("SELECT COUNT(*) FROM evidence WHERE verified = TRUE AND status <> 'INTEGRITY_VIOLATION'"),
        "integrityViolations", count("SELECT COUNT(*) FROM evidence WHERE status = 'INTEGRITY_VIOLATION'"),
        "pendingTransfers", count("SELECT COUNT(*) FROM transfers WHERE status = 'PENDING'"),
        "openAlerts", count("SELECT COUNT(*) FROM alerts WHERE resolved = FALSE"),
        "totalCases", count("SELECT COUNT(*) FROM cases"),
        "activeUsers", count("SELECT COUNT(*) FROM users WHERE active = TRUE")
    );
}
 private long count(String sql){Long n=db.queryForObject(sql,Long.class);return n==null?0:n;}

 
@GetMapping("/users")
List<Map<String, Object>> users(HttpSession s) {
    admin(s);

    return db.queryForList(
        "SELECT " +
        "user_id AS id, " +
        "full_name AS name, " +
        "username, " +
        "email, " +
        "role, " +
        "department, " +
        "active " +
        "FROM users " +
        "ORDER BY full_name"
    );
}

 @GetMapping("/recipients") List<Map<String,Object>> recipients(HttpSession s){return db.queryForList("SELECT user_id AS id,full_name AS name,role FROM users WHERE active=TRUE AND user_id<>? ORDER BY full_name",uid(s));}
 @PostMapping("/users") Map<String,Object> createUser(@RequestBody Map<String,String>b,HttpSession s){
  admin(s);String pass=b.getOrDefault("password","");if(pass.length()<10)throw new IllegalArgumentException("Password must be at least 10 characters.");
  String fullName=b.getOrDefault("name","").trim();String username=b.getOrDefault("username","").trim();String email=b.getOrDefault("email","").trim();
  if(fullName.isBlank()||username.isBlank()||email.isBlank())throw new IllegalArgumentException("Name, username, and email are required.");
  String role=b.getOrDefault("role","INVESTIGATOR");if(!List.of("ADMIN","INVESTIGATOR","ANALYST","CUSTODIAN").contains(role))throw new IllegalArgumentException("Invalid role.");
  String userId="USR-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
  db.update("INSERT INTO users(user_id,full_name,name,username,email,password_hash,role,department,active) VALUES(?,?,?,?,?,?,?,?,TRUE)", userId, fullName, fullName, username, email, encoder.encode(pass), role, b.getOrDefault("department", ""));
  return Map.of("id",userId,"message","User created");
 }
 @PatchMapping("/users/{id}/active") Map<String,String> active(@PathVariable String id,@RequestBody Map<String,Boolean>b,HttpSession s){admin(s);if(db.update("UPDATE users SET active=? WHERE user_id=?",Boolean.TRUE.equals(b.get("active")),id)==0)throw new IllegalArgumentException("User not found");return Map.of("message","User status updated");}

 
@GetMapping("/cases")
List<Map<String, Object>> cases() {
    return db.queryForList(
        "SELECT c.case_id, c.title, c.description, c.created_at, " +
        "u.full_name AS created_by, 'OPEN' AS status, " +
        "(SELECT COUNT(*) FROM evidence e WHERE e.case_id = c.case_id) " +
        "AS evidence_count " +
        "FROM cases c LEFT JOIN users u ON u.user_id = c.created_by " +
        "ORDER BY c.created_at DESC"
    );
}
 @PostMapping("/cases") Map<String,String> createCase(@RequestBody Map<String,String>b,HttpSession s){if(b.get("title")==null||b.get("title").isBlank())throw new IllegalArgumentException("Case title is required");String id="CASE-"+java.time.Year.now()+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase();db.update("INSERT INTO cases(case_id,title,description,created_by) VALUES(?,?,?,?)",id,b.get("title"),b.get("description"),uid(s));return Map.of("caseId",id,"message","Case created");}
 

 
@GetMapping("/evidence")
List<Map<String, Object>> evidence(@RequestParam(required = false) String q) {
    String v = "%" + (q == null ? "" : q.trim()) + "%";
    return db.queryForList(
        "SELECT e.evidence_id,e.case_id,e.name,e.type AS evidence_type,e.original_hash,e.verified,e.created_at AS registered_at,e.uploaded_by,e.status,e.file_path,e.last_verified_at,u.full_name AS custodian_name " +
        "FROM evidence e LEFT JOIN users u ON u.user_id=e.uploaded_by " +
        "WHERE e.evidence_id LIKE ? OR e.name LIKE ? OR e.case_id LIKE ? ORDER BY e.created_at DESC", v,v,v);
}

@GetMapping("/evidence/{id}")
Map<String, Object> evidenceDetail(@PathVariable String id) {
    var rows = db.queryForList(
        "SELECT e.evidence_id,e.case_id,c.title AS case_title,e.name,e.type AS evidence_type,e.original_hash,e.file_path,e.status,e.verified,e.created_at AS registered_at,e.last_verified_at,e.uploaded_by,u.full_name AS custodian_name,0 AS file_size,'' AS description,e.name AS original_filename " +
        "FROM evidence e LEFT JOIN users u ON u.user_id=e.uploaded_by LEFT JOIN cases c ON c.case_id=e.case_id WHERE e.evidence_id=?", id);
    if (rows.isEmpty()) throw new IllegalArgumentException("Evidence not found.");
    return rows.get(0);
}

@PostMapping(value = "/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Transactional
Map<String, Object> register(
        @RequestParam String caseId,
        @RequestParam String name,
        @RequestParam(defaultValue = "OTHER") String type,
        @RequestPart("file") MultipartFile file,
        HttpSession s) throws IOException {

    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException("Choose a file.");
    }
    if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Evidence name is required.");
    }

    Integer caseCount = db.queryForObject(
        "SELECT COUNT(*) FROM cases WHERE case_id = ?",
        Integer.class, caseId
    );
    if (caseCount == null || caseCount == 0) {
        throw new IllegalArgumentException("Case does not exist.");
    }

    Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
    Files.createDirectories(dir);

    String original = file.getOriginalFilename() == null
        ? "upload.bin"
        : Paths.get(file.getOriginalFilename()).getFileName().toString();

    String stored = UUID.randomUUID() + "-" +
        original.replaceAll("[^A-Za-z0-9._-]", "_");

    Path target = dir.resolve(stored).normalize();
    if (!target.startsWith(dir)) {
        throw new IllegalArgumentException("Invalid file name.");
    }

    String evidenceId = "EV-" + java.time.Year.now() + "-" +
        UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    try {
        file.transferTo(target);
        String hash = HashUtil.sha256(target);
        Integer duplicate = db.queryForObject(
            "SELECT COUNT(*) FROM evidence WHERE case_id = ? AND original_hash = ?",
            Integer.class, caseId, hash);
        if (duplicate != null && duplicate > 0) {
            Files.deleteIfExists(target);
            throw new IllegalArgumentException("This exact file is already registered under this case.");
        }

        String actor = uid(s);
        Integer activeUser = db.queryForObject(
            "SELECT COUNT(*) FROM users WHERE user_id = ? AND active = TRUE",
            Integer.class, actor
        );
        if (activeUser == null || activeUser == 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Your account is not active. Sign in again with an active account."
            );
        }

        db.update(
            "INSERT INTO evidence " +
            "(evidence_id, case_id, name, type, original_hash, verified, " +
            "created_at, uploaded_by, file_path, status) " +
            "VALUES (?, ?, ?, ?, ?, FALSE, NOW(), ?, ?, 'REGISTERED')",
            evidenceId, caseId, name.trim(), type, hash, actor, target.toString()
        );
        event(evidenceId, actor, "EVIDENCE_REGISTERED", null, actor,
            "Evidence registered; original SHA-256 captured.");

        return Map.of(
            "evidenceId", evidenceId,
            "caseId", caseId,
            "name", name,
            "type", type,
            "originalHash", hash,
            "status", "REGISTERED"
        );
    } catch (Exception ex) {
        Files.deleteIfExists(target);
        throw ex;
    }
}

 
@PostMapping("/evidence/{id}/verify")
Map<String, Object> verify(@PathVariable String id, HttpSession s)
        throws IOException {

    var rows = db.queryForList(
        "SELECT file_path, original_hash FROM evidence WHERE evidence_id = ?", id
    );

    if (rows.isEmpty()) {
        throw new IllegalArgumentException("Evidence not found.");
    }

    var row = rows.get(0);
    Object filePath = row.get("file_path");
    String originalHash = String.valueOf(row.get("original_hash"));

    boolean exists = filePath != null
        && !String.valueOf(filePath).isBlank()
        && Files.isRegularFile(Paths.get(String.valueOf(filePath)));

    String currentHash = exists
        ? HashUtil.sha256(Paths.get(String.valueOf(filePath)))
        : "MISSING";

    boolean match = exists && currentHash.equalsIgnoreCase(originalHash);
    String status = match ? "VERIFIED" : "INTEGRITY_VIOLATION";

    db.update(
        "UPDATE evidence SET verified = ?, status = ?, last_verified_at = NOW() " +
        "WHERE evidence_id = ?",
        match, status, id
    );

    event(
        id, uid(s), match ? "INTEGRITY_VERIFIED" : "INTEGRITY_VIOLATION",
        null, null, match ? "SHA-256 matches original." : "File missing or hash mismatch."
    );

    if (!match) {
        alert(id, exists ? "HASH_MISMATCH" : "FILE_MISSING",
            exists ? "Current SHA-256 differs from original."
                   : "Evidence file is missing from storage.");
    }

    return Map.of(
        "evidenceId", id,
        "exists", exists,
        "match", match,
        "originalHash", originalHash,
        "currentHash", currentHash,
        "status", status
    );
}

 
@GetMapping("/evidence/{id}/custody")
List<Map<String, Object>> custody(@PathVariable String id) {
    return db.queryForList(
        "SELECT ce.event_id AS id, ce.evidence_id, " +
        "ce.event_type AS action, ce.notes AS details, " +
        "SHA2(CONCAT_WS('|', ce.event_id, ce.evidence_id, ce.actor_user_id, " +
        "ce.from_user_id, ce.to_user_id, ce.event_type, ce.notes, ce.created_at), 256) AS event_hash, " +
        "ce.created_at, " +
        "a.full_name AS actor_name, " +
        "p.full_name AS previous_custodian_name, " +
        "n.full_name AS new_custodian_name " +
        "FROM custody_events ce " +
        "LEFT JOIN users a ON a.user_id = ce.actor_user_id " +
        "LEFT JOIN users p ON p.user_id = ce.from_user_id " +
        "LEFT JOIN users n ON n.user_id = ce.to_user_id " +
        "WHERE ce.evidence_id = ? " +
        "ORDER BY ce.created_at, ce.event_id",
        id
    );
}

 

@GetMapping("/transfers")
List<Map<String, Object>> transfers(HttpSession s) {
    String sql =
        "SELECT t.transfer_id, t.evidence_id, e.name AS evidence_name, " +
        "t.from_user, fu.full_name AS from_name, " +
        "t.to_user, tu.full_name AS to_name, t.remarks, t.status, " +
        "t.requested_at, t.responded_at " +
        "FROM transfers t " +
        "JOIN evidence e ON e.evidence_id = t.evidence_id " +
        "JOIN users fu ON fu.user_id = t.from_user " +
        "JOIN users tu ON tu.user_id = t.to_user ";

    if ("ADMIN".equals(role(s))) {
        return db.queryForList(sql + "ORDER BY t.requested_at DESC");
    }

    return db.queryForList(
        sql + "WHERE t.from_user = ? OR t.to_user = ? " +
        "ORDER BY t.requested_at DESC",
        uid(s), uid(s)
    );
}

@PostMapping("/evidence/{id}/transfers")
@Transactional
Map<String, Object> requestTransfer(
        @PathVariable String id,
        @RequestBody Map<String, Object> body,
        HttpSession s) {

    String from = uid(s);
    Object recipientValue = body.get("toUser");
    if (recipientValue == null) recipientValue = body.get("recipientId");
    if (recipientValue == null) recipientValue = body.get("recipient");
    String to = recipientValue == null ? "" : recipientValue.toString().trim();
    Object remarksValue = body.get("remarks");
    String remarks = remarksValue == null ? "" : remarksValue.toString().trim();

    if (to.isBlank() || from.equals(to)) {
        throw new IllegalArgumentException("Choose a different recipient.");
    }

    var rows = db.queryForList(
        "SELECT uploaded_by, status FROM evidence WHERE evidence_id = ?", id
    );

    if (rows.isEmpty()) {
        throw new IllegalArgumentException("Evidence not found.");
    }

    if (!from.equals(String.valueOf(rows.get(0).get("uploaded_by")))) {
        throw new IllegalArgumentException("Only the current custodian can transfer this evidence.");
    }

    if ("INTEGRITY_VIOLATION".equals(rows.get(0).get("status"))) {
        throw new IllegalArgumentException("Resolve the integrity issue before transferring.");
    }

    Integer pending = db.queryForObject(
        "SELECT COUNT(*) FROM transfers WHERE evidence_id = ? AND status = 'PENDING'",
        Integer.class, id
    );
    if (pending != null && pending > 0) {
        throw new IllegalArgumentException("A transfer is already pending.");
    }

    Integer recipientCount = db.queryForObject(
    "SELECT COUNT(*) FROM users WHERE user_id = ? AND active = TRUE",
    Integer.class,
    to
);

if (recipientCount == null || recipientCount == 0) {
    throw new IllegalArgumentException(
        "Recipient ID '" + to + "' does not match an active user."
    );
}

    db.update(
        "INSERT INTO transfers (evidence_id, from_user, to_user, remarks, status) " +
        "VALUES (?, ?, ?, ?, 'PENDING')",
        id, from, to, remarks
    );

    db.update(
        "UPDATE evidence SET status = 'TRANSFER_PENDING' WHERE evidence_id = ?", id
    );

    event(id, from, "TRANSFER_INITIATED", from, to, remarks);

    return Map.of("message", "Transfer request created.");
}


 
@PostMapping("/transfers/{id}/respond")
@Transactional
Map<String, String> respond(
        @PathVariable long id,
        @RequestBody Map<String, Boolean> body,
        HttpSession s) {

    var rows = db.queryForList(
        "SELECT evidence_id, from_user, to_user, status " +
        "FROM transfers WHERE transfer_id = ? FOR UPDATE", id
    );

    if (rows.isEmpty()) {
        throw new IllegalArgumentException("Transfer not found.");
    }

    var transfer = rows.get(0);
    String actor = uid(s);
    String recipient = String.valueOf(transfer.get("to_user"));
    String sender = String.valueOf(transfer.get("from_user"));
    String evidenceId = String.valueOf(transfer.get("evidence_id"));

    if (!actor.equals(recipient)) {
        throw new org.springframework.web.server.ResponseStatusException(
            HttpStatus.FORBIDDEN, "Only the recipient can respond."
        );
    }

    if (!"PENDING".equals(transfer.get("status"))) {
        throw new IllegalArgumentException("Transfer is no longer pending.");
    }

    boolean accept = Boolean.TRUE.equals(body.get("accept"));

    db.update(
        "UPDATE transfers SET status = ?, responded_at = NOW() WHERE transfer_id = ?",
        accept ? "ACCEPTED" : "REJECTED", id
    );

    if (accept) {
        db.update(
            "UPDATE evidence SET uploaded_by = ?, status = 'REGISTERED' " +
            "WHERE evidence_id = ?", actor, evidenceId
        );
    } else {
        db.update(
            "UPDATE evidence SET status = 'REGISTERED' WHERE evidence_id = ?",
            evidenceId
        );
    }

    event(
        evidenceId, actor,
        accept ? "TRANSFER_ACCEPTED" : "TRANSFER_REJECTED",
        sender, accept ? actor : sender,
        "Transfer #" + id + (accept ? " accepted." : " rejected.")
    );

    return Map.of("message", accept ? "Transfer accepted." : "Transfer rejected.");
}

@GetMapping("/alerts")
List<Map<String, Object>> alerts(
        @RequestParam(required = false) String status) {

    String sql =
        "SELECT a.*, e.name AS evidence_name " +
        "FROM alerts a " +
        "LEFT JOIN evidence e ON e.evidence_id = a.evidence_id ";

    if (status == null || status.isBlank()
            || "ALL".equalsIgnoreCase(status)) {
        return db.queryForList(sql + "ORDER BY a.created_at DESC");
    }

    if ("OPEN".equalsIgnoreCase(status)) {
        return db.queryForList(
            sql + "WHERE a.resolved = FALSE ORDER BY a.created_at DESC"
        );
    }

    if ("RESOLVED".equalsIgnoreCase(status)) {
        return db.queryForList(
            sql + "WHERE a.resolved = TRUE ORDER BY a.created_at DESC"
        );
    }

    throw new IllegalArgumentException("Invalid alert status");
}

 @PostMapping("/alerts/{id}/resolve")
 Map<String, String> resolve(@PathVariable String id, HttpSession s) {
    admin(s);
    int changed = db.update(
        "UPDATE alerts SET resolved = TRUE WHERE alert_id = ? AND resolved = FALSE", id
    );
    if (changed == 0) throw new IllegalArgumentException("Open alert not found. Refresh the alerts page.");
    return Map.of("message", "Alert resolved");
 }
 @GetMapping(value="/reports/evidence.csv",produces="text/csv") ResponseEntity<byte[]> report(){var rows=evidence("");StringBuilder b=new StringBuilder("evidence_id,case_id,name,type,status,custodian,registered_at,sha256\n");for(var r:rows)b.append(csv(r.get("evidence_id"))).append(',').append(csv(r.get("case_id"))).append(',').append(csv(r.get("name"))).append(',').append(csv(r.get("evidence_type"))).append(',').append(csv(r.get("status"))).append(',').append(csv(r.get("custodian_name"))).append(',').append(csv(r.get("registered_at"))).append(',').append(csv(r.get("original_hash"))).append('\n');return ResponseEntity.ok().header("Content-Disposition","attachment; filename=sentinel-evidence-report.csv").contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(b.toString().getBytes(StandardCharsets.UTF_8));}
 private String csv(Object o){return "\""+String.valueOf(o==null?"":o).replace("\"","\"\"")+"\"";}
 @Scheduled(fixedDelayString = "${sentinel.monitor.interval-ms:300000}", initialDelay = 60000)
 void monitor() {
    if (!monitorEnabled) return;
    var rows = db.queryForList(
        "SELECT evidence_id, file_path, original_hash FROM evidence " +
        "WHERE status NOT IN ('CASE_CLOSED', 'INTEGRITY_VIOLATION')"
    );
    for (var row : rows) {
        String id = String.valueOf(row.get("evidence_id"));
        Object pathValue = row.get("file_path");
        try {
            boolean exists = pathValue != null && !String.valueOf(pathValue).isBlank()
                && Files.isRegularFile(Paths.get(String.valueOf(pathValue)));
            boolean matches = exists && HashUtil.sha256(Paths.get(String.valueOf(pathValue)))
                .equalsIgnoreCase(String.valueOf(row.get("original_hash")));
            if (!matches) {
                String type = exists ? "HASH_MISMATCH" : "FILE_MISSING";
                String message = exists
                    ? "Scheduled integrity check detected a hash mismatch."
                    : "Scheduled integrity check found the evidence file missing.";
                db.update(
                    "UPDATE evidence SET status = 'INTEGRITY_VIOLATION', last_verified_at = NOW() WHERE evidence_id = ?",
                    id
                );
                alert(id, type, message);
            }
        } catch (Exception ex) {
            System.err.println("Monitor failed for " + id + ": " + ex.getMessage());
        }
    }
 }

 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Map<String,String>> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Invalid request":e.getMessage()));}
 @ExceptionHandler(DuplicateKeyException.class) ResponseEntity<Map<String,String>> duplicate(DuplicateKeyException e){return ResponseEntity.status(409).body(Map.of("error","Username, email, or ID already exists."));}

 @ExceptionHandler(org.springframework.dao.DataAccessException.class)
 ResponseEntity<Map<String, String>> databaseError(org.springframework.dao.DataAccessException e) {
    System.err.println("Sentinel database operation failed: " + e.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "Database operation failed. Check that the Sentinel schema matches database/schema.sql and inspect the VS Code terminal."));
 }

 @ExceptionHandler(IOException.class)
 ResponseEntity<Map<String, String>> fileError(IOException e) {
    System.err.println("Sentinel file operation failed: " + e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "File storage operation failed. Check the storage/evidence folder permissions and available disk space."));
 }

}
