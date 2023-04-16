package info.neu.infoapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.Filter;

@Configuration
public class ETagConfig {
    @Bean
    public Filter etagFilter() {
        return new ShallowEtagHeaderFilter();
    }
}
