package com.kuet.hub.dto;

//package com.kuet.researchequipmenthub.dto;

import com.kuet.hub.entity.Item;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
public class ItemDto {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Condition is required")
    private Item.Condition condition;

    private Long categoryId;
}