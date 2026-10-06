package cbd.order_tracker.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrintFilesUrlValidatorTest {

	private static boolean valid(String url, Set<String> allowedHosts) {
		try {
			PrintFilesUrlValidator.validate(PrintFilesUrlValidator.normalize(url), allowedHosts);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@Test
	void validatorWithANonEmptyAllowedHostConstantRequiresAnExactHostMatch() {
		Set<String> hosts = Set.of("drive.google.com", "docs.google.com");

		assertThat(valid("https://drive.google.com/drive/folders/a", hosts)).isTrue();
		assertThat(valid("https://DRIVE.GOOGLE.COM/x", hosts)).isTrue();
		assertThat(valid("https://docs.google.com/document/d/1", hosts)).isTrue();
		assertThat(valid("https://www.dropbox.com/s/abc", hosts)).isFalse();
		assertThat(valid("https://drive.google.com.evil.com/x", hosts)).isFalse();
		assertThat(valid("https://evil.com/drive.google.com", hosts)).isFalse();
	}

	@Test
	void validatorDefaultAllowedHostSetIsEmptyMeaningAnyHost() {
		assertThat(PrintFilesUrlValidator.ALLOWED_HOSTS).isEmpty();
		assertThatCode(() -> PrintFilesUrlValidator.validate("https://www.dropbox.com/s/abc"))
				.doesNotThrowAnyException();
	}

	@Test
	void whitespaceAndControlCharactersAreRejectedWithTheSpecificMessage() {
		String expected = "printFilesUrl must not contain whitespace or control characters";

		assertThatThrownBy(() -> PrintFilesUrlValidator.validate("https://example.com/a b"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage(expected);
		assertThatThrownBy(() -> PrintFilesUrlValidator.validate("https://example.com/ab"))
				.isInstanceOf(IllegalArgumentException.class).hasMessage(expected);
	}
}
