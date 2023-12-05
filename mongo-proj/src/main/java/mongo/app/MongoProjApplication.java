package mongo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
public class MongoProjApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoProjApplication.class, args);
    }
    @Component
    public class RouteHandlers {

        private final MovieFluxService movieFluxService;

        public RouteHandlers(MovieFluxService movieFluxService) {
            this.movieFluxService = movieFluxService;
        }

        public Mono<ServerResponse> all(ServerRequest request) {
            return ServerResponse.ok().body(movieFluxService.all(), Movie.class);
        }

        public Mono<ServerResponse> byId(ServerRequest request) {
            return
                    ServerResponse
                            .ok()
                            .body(
                                    movieFluxService.byId(
                                            request.pathVariable("movieId")), Movie.class);
        }

        public Mono<ServerResponse> events(ServerRequest request) {
            return ServerResponse
                    .ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(
                            movieFluxService
                                    .streamOfStreams
                                            (request.pathVariable("movieId")), MovieEvent.class);
        }


    }

    @Bean
    RouterFunction<?> routes(RouteHandlers routeHandlers) {
        return route(GET("/movies"), routeHandlers::all)
                .andRoute(GET("/movies/{movieId}"), routeHandlers::byId)
                .andRoute(GET("/movies/{movieId}/events"), routeHandlers::events);
    }

    @Bean
    CommandLineRunner runner(MovieRepository movieRepository) {

        return args -> {
            movieRepository.deleteAll().subscribe(null, null, () -> {
                Stream.of("some title ", "some movie title", "don2", "mi7", "dom2")
                        .forEach(
                                movie ->
                                {
                                    movieRepository.save(new Movie(UUID.randomUUID().toString(), movie))
                                            .subscribe(System.out::println);
                                }
                        );
            });
        };
    }
}


//@RestController
class MovieRestController {

    private final MovieFluxService movieFluxService;

    MovieRestController(MovieFluxService movieFluxService) {
        this.movieFluxService = movieFluxService;
    }

    @GetMapping("/movies")
    public Flux<Movie> allMovies() {
        return movieFluxService.all();
    }

    @GetMapping("/movies/{movieId}")
    public Mono<Movie> allMovies(@PathVariable String movieId) {
        return movieFluxService.byId(movieId);
    }

    @GetMapping("/movies/{movieId}/events")
    public Flux<MovieEvent> eventsById(@PathVariable String movieId) {
        return movieFluxService.streamOfStreams(movieId);
    }


}


@Service
class MovieFluxService {

    private final MovieRepository movieRepository;

    MovieFluxService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    Flux<MovieEvent> streamOfStreams(String movieId) {
        return byId(movieId)
                .flatMapMany(movie -> {
                    Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
                    Flux<MovieEvent> events = Flux.fromStream(Stream.generate(() -> new MovieEvent(movie, new Date())));

                    return Flux.zip(interval, events).map(Tuple2::getT2);
                });
    }


    Flux<Movie> all() {
        return movieRepository.findAll();
    }

    Mono<Movie> byId(String movieId) {

        return movieRepository.findById(movieId);
    }


}

interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}


@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class Movie {

    @Id
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


