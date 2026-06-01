package com.page;

import com.page.ui.NURSwingApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PageApplication {

	public static void main(String[] args) {
		if (Boolean.getBoolean("page.spring.enabled")) {
			SpringApplication.run(PageApplication.class, args);
			return;
		}

		NURSwingApplication.main(args);
	}

}
