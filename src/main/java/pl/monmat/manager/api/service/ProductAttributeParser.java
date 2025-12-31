package pl.monmat.manager.api.service;

import pl.monmat.manager.api.allegro.dto.AllegroOfferDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ProductAttributeParser {
    private static final Pattern INTERNAL_ID_PATTERN = Pattern.compile("<p>// (.*?)</p>");

    public Map<String, Object> extractAttributes(AllegroOfferDetails offerDetails) {
        Map<String, Object> attributes = new HashMap<>();
        if (offerDetails.category() == null) {
            return attributes;
        }

        String categoryId = offerDetails.category().id();

    }
}
