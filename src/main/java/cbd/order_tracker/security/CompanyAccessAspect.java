package cbd.order_tracker.security;

import cbd.order_tracker.config.RequestUserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Aspect
@Component
public class CompanyAccessAspect {

    @Around("@annotation(cbd.order_tracker.security.CheckCompanyAccess)")
    public Object checkCompany(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        CheckCompanyAccess ann = method.getAnnotation(CheckCompanyAccess.class);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }

        Object details = auth.getDetails();
        if (!(details instanceof RequestUserContext ctx)) {
            throw new AccessDeniedException("Missing user context");
        }

        // Super admins bypass the check
        if (ctx.isSuperAdmin()) {
            return pjp.proceed();
        }

        Object[] args = pjp.getArgs();
        if (args.length <= ann.paramIndex()) {
            throw new IllegalStateException("Invalid param index in @CheckCompanyAccess");
        }

        Object param = args[ann.paramIndex()];
        Set<Long> allowed = ctx.getCompanyIds();

        if (!ann.list()) {
            // Single companyId
            Long companyId = castToLong(param);
            if (companyId == null || !allowed.contains(companyId)) {
                throw new AccessDeniedException("You have no access to company " + companyId);
            }
        } else {
            // List/array/iterable of IDs
            var ids = toLongStream(param).collect(Collectors.toSet());
            if (!allowed.containsAll(ids)) {
                throw new AccessDeniedException("You have no access to all requested companies");
            }
        }

        return pjp.proceed();
    }

    private Long castToLong(Object param) {
        if (param == null) return null;
        if (param instanceof Long l) return l;
        if (param instanceof Integer i) return i.longValue();
        if (param instanceof String s) return Long.valueOf(s);
        throw new IllegalArgumentException("Unsupported companyId type: " + param.getClass());
    }

    private java.util.stream.Stream<Long> toLongStream(Object param) {
        if (param instanceof Collection<?> coll) {
            return coll.stream().map(this::castToLong);
        }
        if (param.getClass().isArray()) {
            Object[] arr = (Object[]) param;
            return java.util.Arrays.stream(arr).map(this::castToLong);
        }
        if (param instanceof Iterable<?> it) {
            return StreamSupport.stream(it.spliterator(), false).map(this::castToLong);
        }
        throw new IllegalArgumentException("Expected a collection/array of companyIds");
    }
}
