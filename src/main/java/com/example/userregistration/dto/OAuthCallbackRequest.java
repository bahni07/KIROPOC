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
public class OAuthCallbackRequest {

    @NotNull(message = "Provider is required")
    private OAuthRegistrationRequest.OAuthProviderType provider;

    @NotBlank(message = "Authorization code is required")
    private String code;

    @NotBlank(message = "State is required")
    private String state;
}
