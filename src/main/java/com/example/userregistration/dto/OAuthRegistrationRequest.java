package com.example.userregistration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthRegistrationRequest {

    @NotNull(message = "Provider is required")
    private OAuthProviderType provider;

    @NotBlank(message = "Redirect URI is required")
    private String redirectUri;

    public enum OAuthProviderType {
        GOOGLE,
        AMAZON
    }
}
