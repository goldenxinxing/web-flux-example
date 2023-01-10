package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
// useless
// @ServletComponentScan(basePackages = "org.example")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("gxx-");
        executor.initialize();
        return executor;
    }

    /**
     * https://www.baeldung.com/register-servlet
     * 而且，想要生效，spring.main.web-application-type= servlet(不能是reactive)
     * @return
     */
    @Bean
    public ServletRegistrationBean<AsyncServlet> exampleServletBean() {
        ServletRegistrationBean<AsyncServlet> bean = new ServletRegistrationBean<>(
                new AsyncServlet(), "/asyncservlet/*");
        bean.setLoadOnStartup(1);
        bean.setAsyncSupported(true);
        return bean;
    }
}