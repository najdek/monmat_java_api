package pl.monmat.manager.api.allegro.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import pl.monmat.manager.api.SystemSetting;
import pl.monmat.manager.api.SystemSettingRepository;

import java.time.LocalDateTime;

@Service
public class AllegroAuthService {
    private static final Logger log = LoggerFactory.getLogger(AllegroAuthService.class);

    private final RestClient restClient;
    private final SystemSettingRepository systemSettingRepository;

    // Cache token to avoid requesting new one every time
    private String cachedAccessToken;
    private LocalDateTime tokenExpiresAt;

    public AllegroAuthService(RestClient.Builder builder, SystemSettingRepository systemSettingRepository) {
        this.restClient = builder.baseUrl("https://allegro.pl").build();
        this.systemSettingRepository = systemSettingRepository;
    }

    public String getAccessToken() {
        // Check if cached token is still valid (with 5 min buffer)
        if (cachedAccessToken != null && tokenExpiresAt != null
                && LocalDateTime.now().plusMinutes(5).isBefore(tokenExpiresAt)) {
            log.debug("Using cached access token");
            return cachedAccessToken;
        }

        log.info("Requesting new access token using refresh_token flow");

        // Request new token using refresh_token flow
        TokenResponse response = refreshAccessToken();

        // Cache the token
        cachedAccessToken = response.accessToken();
        tokenExpiresAt = LocalDateTime.now().plusSeconds(response.expiresIn());

        log.info("Successfully obtained access token, expires in {} seconds", response.expiresIn());

        return cachedAccessToken;
    }

    private TokenResponse refreshAccessToken() {
        // Get credentials and refresh token from database
        String clientId = systemSettingRepository.findById("allegro.client-id")
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new RuntimeException("Allegro client-id not found in system_settings"));

        String clientSecret = systemSettingRepository.findById("allegro.client-secret")
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new RuntimeException("Allegro client-secret not found in system_settings"));

        String refreshToken = systemSettingRepository.findById("allegro.refresh-token")
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new RuntimeException("Allegro refresh-token not found in system_settings. Please add it to database."));

        log.debug("Using refresh token: {}...", refreshToken.substring(0, Math.min(20, refreshToken.length())));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        try {
            TokenResponse response = restClient.post()
                    .uri("/auth/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(h -> h.setBasicAuth(clientId, clientSecret))
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);

            // If Allegro returns a new refresh token, update it in database
            if (response != null && response.refreshToken() != null && !response.refreshToken().equals(refreshToken)) {
                log.info("Updating refresh token in database");
                systemSettingRepository.save(new SystemSetting("allegro.refresh-token", response.refreshToken()));
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to refresh access token. Error: {}", e.getMessage());
            log.error("Please verify that:");
            log.error("  1. allegro.client-id is correct in database");
            log.error("  2. allegro.client-secret is correct in database");
            log.error("  3. allegro.refresh-token is valid and not expired");
            log.error("If refresh token is invalid, you need to obtain a new one using ./get_allegro_token.sh");
            throw new RuntimeException("Failed to refresh Allegro access token", e);
        }
    }

    record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("expires_in")
        int expiresIn,
        @JsonProperty("scope")
        String scope
    ) {}
}
