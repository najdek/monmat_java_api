package pl.monmat.manager.api.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetails implements Serializable {
    private boolean needsInvoice;
    private String companyName;
    private String taxId; // NIP
    private String street;
    private String city;
    private String zipCode;
    private String countryCode;
}