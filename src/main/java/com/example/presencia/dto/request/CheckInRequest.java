package com.example.presencia.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckInRequest {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
