package com.km.bottlecapcollector.api.legacy;

import com.km.bottlecapcollector.api.legacy.model.BottleCapDto;
import com.km.bottlecapcollector.api.legacy.model.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.api.legacy.model.CapPictureDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Legacy controller for backward compatibility.
 * Uses LegacyCollectionAdapter to bridge to the new CollectionService.
 */
@RestController
@CrossOrigin
@AllArgsConstructor
@Slf4j
public class BottleCapController {

    private static final String DEFAULT_PAGE_NUMBER = "0";
    private static final String DEFAULT_PAGE_SIZE = "10";
    private static final String DEFAULT_SORT_BY = "id";
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final String DEFAULT_TEXT = "";

    private final LegacyCollectionAdapter legacyCollectionAdapter;


    @PostMapping("/caps")
    public ResponseEntity<Long> addBottleCap(@RequestParam("name") String capName,
                                             @RequestParam("desc") String description,
                                             @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(legacyCollectionAdapter.addCapItem(capName, description, file));
    }

    @DeleteMapping("/caps/{id}")
    public ResponseEntity<String> deleteBottleCap(@PathVariable Long id) {
        legacyCollectionAdapter.removeCapItem(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/caps/{id}")
    public ResponseEntity<BottleCapDto> getBottleCap(@PathVariable Long id) {
        return ResponseEntity.ok().body(legacyCollectionAdapter.getCapItemDto(id));
    }

    @PutMapping("/caps/{id}")
    public ResponseEntity<BottleCapDto> updateCap(@PathVariable Long id,
                                                  @RequestParam("newName") String newName,
                                                  @RequestParam("newDesc") String newDesc) {
        return ResponseEntity.ok().body(legacyCollectionAdapter.updateCapItemDto(id, newName, newDesc));
    }

    @PostMapping("/validateCap")
    public BottleCapValidationResponseDto validateBottleCap(@RequestParam("name") String capName,
                                                            MultipartFile file) {
        return legacyCollectionAdapter.validateCapItem(capName, file);
    }

    @PostMapping("/whatCapAreYou")
    public ResponseEntity<BottleCapDto> whatCapAreYou(@RequestParam("name") String capName,
                                                      MultipartFile file) {
        return ResponseEntity.ok().body(legacyCollectionAdapter.validateWhatCapYouAre(capName, file));
    }

    @GetMapping("/caps")
    public List<BottleCapDto> getBottleCaps() {
        return legacyCollectionAdapter.getAllBottleCapsDto();
    }

    @GetMapping("/caps-page")
    public List<BottleCapDto> getBottleCapsPage(
            @RequestParam(value = "pageNo", defaultValue = DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = DEFAULT_SORT_DIRECTION, required = false) String sortDir,
            @RequestParam(value = "searchText", defaultValue = DEFAULT_TEXT, required = false) String searchText) {
        return legacyCollectionAdapter.findCapByText(pageNo, pageSize, sortBy, sortDir, searchText);
    }

    @GetMapping("/links")
    public List<CapPictureDto> getBottleCapsLinks() {
        return legacyCollectionAdapter.getAllBottleCapsLinks();
    }

    @GetMapping("/links-page/")
    public List<CapPictureDto> loadBottleCapsPage(
            @RequestParam(value = "pageNo", defaultValue = DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = DEFAULT_SORT_DIRECTION, required = false) String sortDir) {
        return legacyCollectionAdapter.findCapsPaginated(pageNo, pageSize, sortBy, sortDir);
    }

    @GetMapping("/caps/total")
    public long getCapAmount() {
        return legacyCollectionAdapter.getCapAmount();
    }
}
