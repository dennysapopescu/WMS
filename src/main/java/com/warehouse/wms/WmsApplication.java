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
		};
	}

}
