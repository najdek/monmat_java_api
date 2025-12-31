package pl.monmat.manager.api.allegro.service;

import org.springframework.stereotype.Service;
import pl.monmat.manager.api.allegro.api.AllegroOfferDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
        attributes.put("categoryId", offerDetails.category().id());
        extractInternalId(offerDetails).ifPresent(id -> attributes.put("internalId", id));
        return attributes;
    }

    private Optional<String> extractInternalId(AllegroOfferDetails offerDetails) {
        if (offerDetails.description() == null || offerDetails.description().sections() == null) {
            return Optional.empty();
        }
        for (var section : offerDetails.description().sections()) {
            if (section.items() == null) continue;
            for (var item : section.items()) {
                if (item.content() == null) continue;
                Matcher matcher = INTERNAL_ID_PATTERN.matcher(item.content());
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                }
            }
        }
        return Optional.empty();
    }
}
