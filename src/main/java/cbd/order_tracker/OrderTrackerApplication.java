package cbd.order_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cbd.order_tracker")
public class OrderTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderTrackerApplication.class, args);
	}

}
