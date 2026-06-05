package cbd.order_tracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PastOrPresentUtcValidator.class)
public @interface PastOrPresentUtc {
    String message() default "must be a date/time in the past or present (UTC)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
