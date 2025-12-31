package pl.monmat.manager.api.allegro.api;

public record Offer(String id, String name, External external) {
    public record External(String id) {
    }
}
