package com.digiaudit.grcone;

import com.digiaudit.grcone.runtime.LegacyRuntimeQuarantineTypeFilter;
import com.digiaudit.grcone.runtime.OracleConnectionLog;
import com.digiaudit.grcone.runtime.StageDatabaseCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = "com.digiaudit.grcone",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.CUSTOM,
                classes = LegacyRuntimeQuarantineTypeFilter.class
        )
)
@EnableJpaRepositories(
        basePackages = "com.digiaudit.grcone",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.CUSTOM,
                classes = LegacyRuntimeQuarantineTypeFilter.class
        )
)
public class GrconeApiApplication {

    public static void main(String[] args) {
        if (StageDatabaseCommand.handles(args)) {
            StageDatabaseCommand.run(args[0]);
            return;
        }
        SpringApplication application = new SpringApplication(GrconeApiApplication.class);
        application.addInitializers(context -> OracleConnectionLog.logStartupTarget(context.getEnvironment()));
        application.run(args);
    }

}
