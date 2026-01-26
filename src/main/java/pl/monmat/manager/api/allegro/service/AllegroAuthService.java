package pl.monmat.manager.api.allegro.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import pl.monmat.manager.api.common.SystemSetting;
import pl.monmat.manager.api.common.SystemSettingRepository;

import java.time.LocalDateTime;

@Service
public class AllegroAuthService {
    private static final Logger log = LoggerFactory.getLogger(AllegroAuthService.class);
    private final RestClient restClient;
    private final SystemSettingRepository systemSettingRepository;
    private String cachedAccessToken;
    private LocalDateTime tokenExpiresAt;

    public AllegroAuthService(RestClient.Builder builder, SystemSettingRepository systemSettingRepository) {
        this.restClient = builder.baseUrl("https://allegro.pl").build();
        this.systemSettingRepository = systemSettingRepository;
    }

    public String getAccessToken() {
        if (isTokenValid()) {
            log.debug("Using cached access token");
            return cachedAccessToken;
        }
        log.info("Requesting new access token using refresh_token flow");
        TokenResponse response = refreshAccessToken();
        cacheToken(response);
        log.info("Successfully obtained access token, expires in {} seconds", response.expiresIn());
        return cachedAccessToken;
    }

    private boolean isTokenValid() {
        return cachedAccessToken != null
                && tokenExpiresAt != null
                && LocalDateTime.now().plusMinutes(5).isBefore(tokenExpiresAt);
    }

    private void cacheToken(TokenResponse response) {
        cachedAccessToken = response.accessToken();
        tokenExpiresAt = LocalDateTime.now().plusSeconds(response.expiresIn());
    }

    private TokenResponse refreshAccessToken() {
        String clientId = getSetting("allegro.client-id", "Allegro client-id not found in system_settings");
        String clientSecret = getSetting("allegro.client-secret", "Allegro client-secret not found in system_settings");
        String refreshToken = getSetting("allegro.refresh-token", "Allegro refresh-token not found in system_settings");
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
            updateRefreshTokenIfNeeded(response, refreshToken);
            return response;
        } catch (Exception e) {
            log.error("Failed to refresh access token: {}", e.getMessage());
            log.error("Please verify allegro.client-id, allegro.client-secret, and allegro.refresh-token in database");
            throw new RuntimeException("Failed to refresh Allegro access token", e);
        }
    }

    private String getSetting(String key, String errorMessage) {
        return systemSettingRepository.findById(key)
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new RuntimeException(errorMessage));
    }

    private void updateRefreshTokenIfNeeded(TokenResponse response, String currentRefreshToken) {
        if (response != null && response.refreshToken() != null && !response.refreshToken().equals(currentRefreshToken)) {
            log.info("Updating refresh token in database");
            systemSettingRepository.save(new SystemSetting("allegro.refresh-token", response.refreshToken()));
        }
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") int expiresIn,
            @JsonProperty("scope") String scope
    ) {
    }
}
