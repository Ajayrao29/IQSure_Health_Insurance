
package org.hartford.iqsure.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Data
@Configuration
@ConfigurationProperties(prefix = "iqsure")
public class AppConfig {
    private double maxDiscountCap = 50.0;
    private int pointsPerCorrectAnswer = 10;
    private int pointsPerLevel = 100;
}