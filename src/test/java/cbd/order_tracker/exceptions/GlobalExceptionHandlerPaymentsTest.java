package cbd.order_tracker.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerPaymentsTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void malformedDateReturns400NotA500AsProblemDetailWithDescription() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", LocalDate.class, "from", null, new IllegalArgumentException("bad date"));

        ProblemDetail pd = handler.handleTypeMismatch(ex);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties()).containsKey("description");
        assertThat(pd.getProperties().get("description")).isEqualTo("Invalid value for parameter 'from'");
    }

    @Test
    void typeMismatchHandlerReturnsA400ProblemDetail() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "x", Integer.class, "page", null, null);

        ProblemDetail pd = handler.handleTypeMismatch(ex);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties().get("description")).isNotNull();
        assertThat(pd.getDetail()).isEqualTo(pd.getProperties().get("description"));
    }

    @Test
    void validationFailureMapsToA400ProblemDetail() {
        IllegalArgumentException ex = new IllegalArgumentException("Payment amount must be greater than zero");

        ProblemDetail pd = handler.handleIllegalArgumentException(ex);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties().get("description")).isEqualTo("Payment amount must be greater than zero");
    }
}
