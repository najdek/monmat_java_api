package pl.monmat.manager.api.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {
    private String firstName;
    private String lastName;
    private String companyName;
    private String phoneNumber;
    private String street;
    private String city;
    private String zipCode;
    private String countryCode;
}