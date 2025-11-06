package com.bank.iolog.annotation;

import com.bank.iolog.config.IOLoggerAutoConfiguration;
import com.bank.iolog.config.IOLoggerDataSourceConfig;
import com.bank.iolog.config.IOLoggerFilterConfig;
import com.bank.iolog.config.IORabbitLoggerAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({IOLoggerAutoConfiguration.class, IOLoggerDataSourceConfig.class, IORabbitLoggerAutoConfiguration.class,
IOLoggerFilterConfig.class})
public @interface EnableIOLogger {
}
