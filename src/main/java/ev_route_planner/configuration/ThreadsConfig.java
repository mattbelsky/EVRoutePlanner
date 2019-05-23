package ev_route_planner.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static ev_route_planner.configuration.ConfigConstants.EXECUTOR;

@Configuration
@EnableAsync
public class ThreadsConfig {

    @Bean(EXECUTOR)
    ThreadPoolTaskExecutor threadPoolTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(30);
        executor.setThreadNamePrefix("openchargemaps_query_thread");
        executor.initialize();

        return executor;
    }
}
