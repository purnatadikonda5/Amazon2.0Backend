package com.purna.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {
    
    @NotBlank(message = "Refresh token cannot be empty")
    private String refreshToken;
    
    @NotBlank(message = "Email is required to validate rotation")
    private String email;
}
