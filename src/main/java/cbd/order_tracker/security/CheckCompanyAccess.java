package cbd.order_tracker.security;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckCompanyAccess {
    /**
     * Which method parameter index holds the company id(s).
     * Default 0 = the first parameter.
     */
    int paramIndex() default 0;

    /**
     * If true, the parameter is a collection/array of IDs; all must be allowed.
     */
    boolean list() default false;
}
