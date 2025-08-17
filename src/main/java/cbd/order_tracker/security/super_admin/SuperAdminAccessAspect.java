package cbd.order_tracker.security.super_admin;

import cbd.order_tracker.config.RequestUserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SuperAdminAccessAspect {

    @Around("@annotation(cbd.order_tracker.security.super_admin.CheckSuperAdmin)")
    public Object checkSuperAdmin(ProceedingJoinPoint pjp) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getDetails() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }

        Object details = auth.getDetails();
        if (!(details instanceof RequestUserContext ctx)) {
            throw new AccessDeniedException("Missing user context");
        }

        if (!ctx.isSuperAdmin()) {
            throw new AccessDeniedException("You must be a super admin to access this resource");
        }

        return pjp.proceed();
    }
}
