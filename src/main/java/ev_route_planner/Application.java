package ev_route_planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

// To deploy to remote Tomcat server uncomment class extension and SpringApplicationBuilder configure() method.

@SpringBootApplication
public class Application /*extends SpringBootServletInitializer*/ {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

//    @Override
//    protected SpringApplicationBuilder configure (SpringApplicationBuilder application){
//        return application.sources(ev_route_planner.Application.class);
//    }

}

// Ryan: all in all looks like great work Matt! I made some comments throughout and tried to answer all of your
// in-line questions. The code needs to be cleaned up a bit, and polished, but it's very work. A fantastic start.