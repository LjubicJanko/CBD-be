package cbd.order_tracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class PastOrPresentUtcValidator implements ConstraintValidator<PastOrPresentUtc, LocalDateTime> {

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return !value.isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
