package pl.monmat.manager.api.service;

import org.springframework.stereotype.Service;
import pl.monmat.manager.api.allegro.dto.AllegroOfferDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductAttributeParser {
    private static final Pattern INTERNAL_ID_PATTERN = Pattern.compile("<p>// (.*?)</p>");

    public Map<String, Object> extractAttributes(AllegroOfferDetails offerDetails) {
        Map<String, Object> attributes = new HashMap<>();
        if (offerDetails.category() == null) {
            return attributes;
        }

        String categoryId = offerDetails.category().id();
        attributes.put("categoryId", categoryId);

        // Extract internal ID from description if available
        if (offerDetails.description() != null && offerDetails.description().sections() != null) {
            for (var section : offerDetails.description().sections()) {
                if (section.items() != null) {
                    for (var item : section.items()) {
                        if (item.content() != null) {
                            Matcher matcher = INTERNAL_ID_PATTERN.matcher(item.content());
                            if (matcher.find()) {
                                attributes.put("internalId", matcher.group(1));
                                break;
                            }
                        }
                    }
                }
            }
        }

        return attributes;
    }
}
