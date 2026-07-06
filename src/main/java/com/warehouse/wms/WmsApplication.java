package com.warehouse.wms;

import com.warehouse.wms.model.User;
import com.warehouse.wms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class WmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(WmsApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {

			// userRepository.deleteAll();

			// --- ADMIN ---
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = new User();
				admin.setUsername("admin");
				// IMPORTANT: parola trebuie trecută prin encoder
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setRole("ROLE_ADMIN");
				admin.setFullName("Administrator");
				userRepository.save(admin);
				System.out.println("Utilizator admin creat cu succes!");
			}

			// --- OPERATOR ---
			if (userRepository.findByUsername("operator").isEmpty()) {
				User op = new User();
				op.setUsername("operator");
				op.setPassword(passwordEncoder.encode("operator123"));
				op.setRole("ROLE_OPERATOR");
				op.setFullName("Warehouse Operator");
				userRepository.save(op);
			}

			// --- VIEWER ---
			if (userRepository.findByUsername("viewer").isEmpty()) {
				User view = new User();
				view.setUsername("viewer");
				view.setPassword(passwordEncoder.encode("viewer123"));
				view.setRole("ROLE_VIEWER");
				view.setFullName("Guest Viewer");
				userRepository.save(view);
			}
		};
	}

}
