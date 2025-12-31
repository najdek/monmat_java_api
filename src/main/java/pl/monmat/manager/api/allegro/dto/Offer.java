package pl.monmat.manager.api.allegro.dto;

public record Offer(
        String id,
        String name,
        External external
) {
    public record External(String id) {}
}
