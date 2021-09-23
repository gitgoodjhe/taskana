package pro.taskana.example.boot;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Example Application showing a minimal implementation of the taskana REST service. */
@EnableScheduling
@SpringBootApplication
@ComponentScan("pro.taskana")
public class ExampleRestApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExampleRestApplication.class, args);
  }
}
