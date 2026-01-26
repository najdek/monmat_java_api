package pl.monmat.manager.api.common.model;

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
    private String taxId;
    private String street;
    private String city;
    private String zipCode;
    private String countryCode;
}
