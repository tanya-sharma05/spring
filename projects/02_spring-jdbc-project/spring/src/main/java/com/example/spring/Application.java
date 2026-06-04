package com.example.spring;

import com.example.spring.model.Alien;
import com.example.spring.repo.AlienRepo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(Application.class, args);

		Alien a1 = context.getBean(Alien.class);
		a1.setId(104);
		a1.setName("Test4");
		a1.setTech("JS");

		AlienRepo repo = context.getBean(AlienRepo.class);

		repo.save(a1);

		System.out.println(repo.findAll());
	}

}
