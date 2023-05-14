package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.dto.*;
import com.km.bottlecapcollector.service.BottleCapService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@CrossOrigin
@AllArgsConstructor
@Slf4j
public class BottleCapController {

    private final BottleCapService bottleCapService;


    @PostMapping("/caps")
    public ResponseEntity<Long> addBottleCap(@RequestParam("name") String capName, @RequestParam("desc") String description,
                                             @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bottleCapService.addCapItem(capName, description, file));
    }

    @DeleteMapping("/caps/{id}")
    public ResponseEntity<String> deleteBottleCap(@PathVariable Long id) {
        bottleCapService.removeCapItem(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/caps/{id}")
    public ResponseEntity<BottleCapDto> getBottleCap(@PathVariable Long id) {
        return ResponseEntity.ok().body(bottleCapService.getCapItemDto(id));
    }

    @PutMapping("/caps/{id}")
    public ResponseEntity<BottleCapDto> updateCap(@PathVariable Long id, @RequestParam("newName") String newName,
                                                  @RequestParam("newDesc") String newDesc) {
        return ResponseEntity.ok().body(bottleCapService.updateCapItemDto(id, newName, newDesc));
    }

    @PostMapping("/validateCap")
    public BottleCapValidationResponseDto validateBottleCap(@RequestParam("name") String capName, MultipartFile file) {
        return bottleCapService.validateCapItem(capName, file);
    }

    @PostMapping("/whatCapAreYou")
    public ResponseEntity<BottleCapDto> whatCapAreYou(@RequestParam("name") String capName, MultipartFile file) {
        return ResponseEntity.ok().body(bottleCapService.validateWhatCapYouAre(capName, file));
    }


    @GetMapping("/caps")
    public List<BottleCapDto> getBottleCaps() {
        return bottleCapService.getAllBottleCapsDto();
    }

    @GetMapping("/caps-page")
    public List<BottleCapDto> getBottleCapsPage(
            @RequestParam(value = "pageNo", defaultValue = BottleCapService.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = BottleCapService.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = BottleCapService.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = BottleCapService.DEFAULT_SORT_DIRECTION, required = false) String sortDir,
            @RequestParam(value = "searchText", defaultValue = BottleCapService.DEFAULT_TEXT, required = false) String searchText) {
        return bottleCapService.findCapByText(pageNo, pageSize, sortBy, sortDir, searchText);
    }

    @GetMapping("/links")
    public List<CapPictureDto> getBottleCapsLinks() {
        return bottleCapService.getAllBottleCapsLinks();
    }

    @GetMapping("/links-page/")
    public List<CapPictureDto> loadBottleCapsPage(
            @RequestParam(value = "pageNo", defaultValue = BottleCapService.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = BottleCapService.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = BottleCapService.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = BottleCapService.DEFAULT_SORT_DIRECTION, required = false) String sortDir) {

        return bottleCapService.findCapsPaginated(pageNo, pageSize, sortBy, sortDir);
    }

    @GetMapping("/caps/total")
    public long getCapAmount(){
        return bottleCapService.getCapAmount();
    }
}
