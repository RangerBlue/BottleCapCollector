package com.km.bottlecapcollector.firestore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for acknowledging a temporary bottle cap.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcknowledgeCapRequest {

    private String name;

    private String description;
}
