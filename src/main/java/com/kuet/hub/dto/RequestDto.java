package com.kuet.hub.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating and submitting equipment requests.
 * Used to transfer request information from the frontend form to the controller
 * without exposing the full Request entity.
 */
@Getter
@Setter
@NoArgsConstructor
public class RequestDto {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "A message is required")
    private String message;
}
