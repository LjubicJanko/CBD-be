package cbd.order_tracker.model;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The migration scripts are checked at string level only: they cannot be executed here (no MySQL).
 */
class PaymentReportInfrastructureTest {

    private static final Path MIGRATIONS = Path.of("src", "main", "resources", "db", "migration");

    @Test
    void paymentDateIndexIsDeclared() {
        Table table = Payment.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.indexes()).hasSize(1);
        Index index = table.indexes()[0];
        assertThat(index.name()).isEqualTo("idx_payment_date");
        assertThat(index.columnList()).isEqualTo("payment_date");
    }

    @Test
    void backfillMigrationScriptExistsWithTheAgreedLogic() throws IOException {
        String sql = Files.readString(MIGRATIONS.resolve("backfill_order_balances.sql"));
        String update = sql.substring(sql.indexOf("UPDATE order_record"));

        assertThat(update).contains("WHERE sale_price IS NOT NULL");
        assertThat(update).contains("amount_left_to_pay          = sale_price - COALESCE(amount_paid, 0)");
        assertThat(update).contains("sale_price_with_tax - COALESCE(amount_paid, 0)");
        assertThat(update).contains("WHEN sale_price_with_tax IS NULL THEN amount_left_to_pay_with_tax");
        assertThat(update).contains("price_difference            = sale_price - COALESCE(acquisition_cost, 0)");
        // the executable statement never assigns amount_paid
        assertThat(update).doesNotContain("SET amount_paid").doesNotContain(" amount_paid =");
    }

    @Test
    void paymentDateIndexMigrationScriptCreatesTheIndexIdempotently() throws IOException {
        String sql = Files.readString(MIGRATIONS.resolve("add_payment_date_index.sql"));

        assertThat(sql).contains("information_schema.statistics")
                .contains("index_name = 'idx_payment_date'")
                .contains("CREATE INDEX idx_payment_date ON payment (payment_date)")
                .contains("PREPARE stmt FROM @ddl");
    }
}
