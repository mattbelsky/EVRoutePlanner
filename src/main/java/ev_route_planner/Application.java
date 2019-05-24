package ev_route_planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

// To deploy to remote Tomcat server uncomment class extension and SpringApplicationBuilder configure() method.

@SpringBootApplication
@EnableCaching
public class Application /*extends SpringBootServletInitializer*/ {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    ThreadPoolTaskExecutor threadPoolTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(30);
        executor.setThreadNamePrefix("ocm-query-");
        executor.initialize();

        return executor;
    }

//    @Override
//    protected SpringApplicationBuilder configure (SpringApplicationBuilder application){
//        return application.sources(ev_route_planner.Application.class);
//    }

}