package hu.crs.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class ReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveApplication.class, args);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(DatabaseClient databaseClient, CustomerRepository customerRepository) {
		var ddlUpdate = databaseClient.sql("create table CUSTOMER (id serial primary key not null, name varchar(255) not null)").fetch().rowsUpdated();

		return event -> {
			var names = Flux.just("Andrew", "Bob", "Cecil");
			var customers = names.map(name -> new Customer(null, name));
			var saved = customers.flatMap(customerRepository::save);

			ddlUpdate
					.thenMany(customerRepository.deleteAll())
					.thenMany(saved)
					.thenMany(customerRepository.findAll())
					.subscribe(System.out::println);
		};
	}
}


record Customer(@Id Integer id, String name) {};

@RestController
record CustomerController(CustomerRepository customerRepository) {
	@GetMapping("/customers")
	Flux<Customer> customers() {
		return customerRepository.findAll();
	}
}

@Repository
interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {
}
