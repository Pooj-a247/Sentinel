
package com.sentinel;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;

@Configuration
class AppConfig implements WebMvcConfigurer {

    private final ApiAuthInterceptor interceptor;

    AppConfig(ApiAuthInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/me");
    }
}

@Component
class ApiAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest req,
            HttpServletResponse res,
            Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }

        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Please sign in.\"}");
            return false;
        }

        return true;
    }
}

@Component
class FirstRunAdmin implements org.springframework.boot.ApplicationRunner {

    private final JdbcTemplate db;
    private final PasswordEncoder encoder;

    @org.springframework.beans.factory.annotation.Value("${SENTINEL_ADMIN_USERNAME:admin}")
    String username;

    @org.springframework.beans.factory.annotation.Value("${SENTINEL_ADMIN_PASSWORD:}")
    String password;

    FirstRunAdmin(JdbcTemplate db, PasswordEncoder encoder) {
        this.db = db;
        this.encoder = encoder;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

        if (count != null && count == 0) {
            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                    "No Sentinel users exist. Set SENTINEL_ADMIN_PASSWORD before starting the application for the first time."
                );
            }
            String userId = "USR-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            db.update(
                    "INSERT INTO users(user_id,full_name,name,username,email,password_hash,role,department,active) VALUES(?,?,?,?,?,?,?,?,TRUE)",
                    userId,
                    "Sentinel Administrator",
                    "Sentinel Administrator",
                    username,
                    "admin@sentinel.local",
                    encoder.encode(password),
                    "ADMIN",
                    "Administration"
            );

            System.out.println(
                    "First-run admin created: " + username
                    + " (change password before non-demo use)."
            );
        }
    }
}
