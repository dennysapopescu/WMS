package com.warehouse.wms;

import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(WmsApplication.class, args);
	}

	@Bean
	@Profile({"dev", "docker"})
	CommandLineRunner initDatabase(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${wms.seed.admin-password:${ADMIN_PASSWORD:admin123}}") String adminPassword,
			@Value("${wms.seed.operator-password:${OPERATOR_PASSWORD:operator123}}") String operatorPassword,
			@Value("${wms.seed.viewer-password:${VIEWER_PASSWORD:viewer123}}") String viewerPassword
	) {
		return args -> {

			// --- ADMIN ---
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode(adminPassword));
				admin.setRole("ROLE_ADMIN");
				admin.setFullName("Administrator");
				userRepository.save(admin);
				System.out.println("Admin seed user initialized successfully.");
			}

			// --- OPERATOR ---
			if (userRepository.findByUsername("operator").isEmpty()) {
				User op = new User();
				op.setUsername("operator");
				op.setPassword(passwordEncoder.encode(operatorPassword));
				op.setRole("ROLE_OPERATOR");
				op.setFullName("Warehouse Operator");
				userRepository.save(op);
				System.out.println("Operator seed user initialized successfully.");
			}

			// --- VIEWER ---
			if (userRepository.findByUsername("viewer").isEmpty()) {
				User view = new User();
				view.setUsername("viewer");
				view.setPassword(passwordEncoder.encode(viewerPassword));
				view.setRole("ROLE_VIEWER");
				view.setFullName("Guest Viewer");
				userRepository.save(view);
				System.out.println("Viewer seed user initialized successfully.");
			}
		};
	}

}

