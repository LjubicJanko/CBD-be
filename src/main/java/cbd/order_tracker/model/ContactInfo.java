package cbd.order_tracker.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class ContactInfo {
    private String fullName;
    private String phoneNumber;
    private String address;
    private String zipCode;
    private String city;
}
