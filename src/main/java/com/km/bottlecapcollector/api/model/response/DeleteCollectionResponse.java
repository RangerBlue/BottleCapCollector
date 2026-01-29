package com.km.bottlecapcollector.api.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response for delete collection operation")
public class DeleteCollectionResponse {

    @Schema(description = "Message describing the result", example = "Collection deletion initiated. Items are being deleted in the background.")
    private String message;
}
