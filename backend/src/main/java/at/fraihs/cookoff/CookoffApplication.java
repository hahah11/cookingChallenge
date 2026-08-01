package at.fraihs.cookoff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(sharedModules = "shared")
@SpringBootApplication
public class CookoffApplication {

	public static void main(String[] args) {
		SpringApplication.run(CookoffApplication.class, args);
	}

}
