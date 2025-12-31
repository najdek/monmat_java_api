package pl.monmat.manager.api.allegro.dto;

import java.util.List;

public record AllegroOfferDetails(
    String id,
    Category category,
    List<Parameter> parameters,
    Description description
){
    public record Category(
        String id,
        String name
    ){}

    public record Parameter(
        String id,
        List<String> valueIds,
        List<String> values
    ){}

    public record Description(
        List<Section> sections
    ){}

    public record Section(
        List<SectionItem> items
    ){}

    public record SectionItem(
        String type,
        String content
    ){}
}
