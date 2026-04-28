
package com.example.ATSCIRCLE.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

@Configuration
public class MongoDateTimeConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
            new Converter<Date, LocalDateTime>() {
                @Override
                public LocalDateTime convert(Date source) {
                    return source == null ? null :
                        LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
                }
            },
            new Converter<LocalDateTime, Date>() {
                @Override
                public Date convert(LocalDateTime source) {
                    return source == null ? null :
                        Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
                }
            }
        ));
    }
}
