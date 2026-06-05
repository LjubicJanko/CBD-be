package cbd.order_tracker.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineTest {

	@Test
	void zeroDistance() {
		double d = AttendanceServiceImpl.haversineMeters(44.787197, 20.457273, 44.787197, 20.457273);
		assertThat(d).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
	}

	@Test
	void oneDegreeLat_isAbout111Km() {
		double d = AttendanceServiceImpl.haversineMeters(0.0, 0.0, 1.0, 0.0);
		// One degree of latitude ~ 111 km regardless of longitude.
		assertThat(d).isCloseTo(111_195.0, org.assertj.core.data.Offset.offset(50.0));
	}

	@Test
	void shortNorthwardOffset_isCloseToExpected() {
		// 100m north along latitude: dLat = 100 / 111320 deg
		double dLat = 100.0 / 111_320.0;
		double d = AttendanceServiceImpl.haversineMeters(44.787197, 20.457273, 44.787197 + dLat, 20.457273);
		assertThat(d).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.5));
	}
}
