
package org.hartford.iqsure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class IQsureApplication {
    public static void main(String[] args) {
        SpringApplication.run(IQsureApplication.class, args);
    }
}