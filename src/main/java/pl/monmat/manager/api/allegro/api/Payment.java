package pl.monmat.manager.api.allegro.api;

import java.time.Instant;

public record Payment(String id, String type, String provider, Instant finishedAt, Price paidAmount) {
}
