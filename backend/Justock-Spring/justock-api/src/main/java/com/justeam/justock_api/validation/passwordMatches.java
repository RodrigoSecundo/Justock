package com.justeam.justock_api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = passwordMatchesValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface passwordMatches {
    String message() default "As senhas não coincidem!";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
