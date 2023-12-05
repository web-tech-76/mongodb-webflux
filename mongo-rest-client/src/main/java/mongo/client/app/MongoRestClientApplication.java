package mongo.client.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@SpringBootApplication
public class MongoRestClientApplication {

    @Bean
    WebClient webClient() {
        return WebClient.create();
    }


    @Bean
    CommandLineRunner commandLineRunner(WebClient webClient) {
        return args -> {
            webClient.get()
                    .uri("http://localhost:8080/movies")
                    .exchangeToFlux(clientResponse -> clientResponse.bodyToFlux(Movie.class))
                    .filter(movie -> movie.getTitle().equalsIgnoreCase("mi7"))
                    .subscribe(filteredMovie-> {
                        webClient.get()
                                .uri("http://localhost:8080/movies/" +filteredMovie.getId()+ "/events")
                                .exchangeToFlux(clientResponse2 -> clientResponse2.bodyToFlux(MovieEvent.class)
                                ).subscribe(System.out::println);
                    });
        };
    }


    public static void main(String[] args) {

        SpringApplication.run(MongoRestClientApplication.class, args);
    }


}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Movie {

    private String id;

    private String title;

}


@Data
@NoArgsConstructor
@AllArgsConstructor
class MovieEvent {

    private Movie movie;

    private Date when;


}
