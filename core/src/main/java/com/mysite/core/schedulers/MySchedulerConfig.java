package com.mysite.core.schedulers;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "MySchedulerConfig OCD",
        description = "my custom config for custom scheduler"
)
public @interface MySchedulerConfig {

    @AttributeDefinition(
            name = "scheduler name",
            description = "name of my scheduler",
            type = AttributeType.STRING
    )
    String schedulerName() default "myScheduler";

    @AttributeDefinition(
            name = "cron job",
            description = "enter the cron job expression",
            type = AttributeType.STRING
    )
    String schedulerCronExp() default "0 * * * * ?";

    @AttributeDefinition(
            name = "enable scheduler",
            description = "checkbox to enable scheduler",
            type = AttributeType.BOOLEAN
    )
    boolean schedulerEnabled() default false;

    @AttributeDefinition(
            name = "additional prop",
            description = "additional prop",
            type = AttributeType.STRING
    )
    String schedulerAddProp() default "";
}
