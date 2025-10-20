package github.fekom.bond;

import org.springframework.boot.SpringApplication;

public class TestBondApplication {

	public static void main(String[] args) {
		SpringApplication.from(BondApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
