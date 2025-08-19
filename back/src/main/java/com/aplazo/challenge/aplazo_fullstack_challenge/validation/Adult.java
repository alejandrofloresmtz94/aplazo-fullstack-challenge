package com.aplazo.challenge.aplazo_fullstack_challenge.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AdultValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adult {

    String message() default "Customer must be between 18 and 65 years old";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}