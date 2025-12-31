package pl.monmat.manager.api.allegro.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import pl.monmat.manager.api.SystemSetting;
import pl.monmat.manager.api.SystemSettingRepository;

public class AllegroAuthService {
    @Value("${allegro.client-id}") private String clientId;
    @Value("${allegro.client-secret}") private String clientSecret;

    private final RestClient restClient;
    private final SystemSettingRepository systemSettingRepository;

    public AllegroAuthService(RestClient.Builder builder, SystemSettingRepository systemSettingRepository) {
        this.restClient = builder.baseUrl("https://allegro.pl").build();
        this.systemSettingRepository = systemSettingRepository;
    }

    public String getAccessToken() {
        String refreshToken = systemSettingRepository.findById("allegro.refresh-token")
                .map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new RuntimeException("Allegro refresh token not found"));

        TokenResponse res = refreshToken(refreshToken);
        systemSettingRepository.save(new SystemSetting("allegro.refresh-token", res.refreshToken()));
        return res.accessToken();
    }

    private TokenResponse refreshToken(String token) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", token);

        return restClient.post().uri("/auth/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .body(body)
                .retrieve().body(TokenResponse.class);
    }

    record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken


    ) {}
}
