package com.example.festival.coverletter.controller;

import com.example.festival.coverletter.entity.CoverLetter;
import com.example.festival.coverletter.service.CoverLetterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coverletter")
public class CoverLetterController {

    private final CoverLetterService service;

    public CoverLetterController(CoverLetterService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public List<CoverLetter> getAll() {
        return service.getAllCoverLetters();
    }

    @GetMapping("/{id}")
    public CoverLetter getById(@PathVariable Long id) {
        return service.getCoverLetter(id);
    }

    @PostMapping("/create")
    public CoverLetter create(@RequestBody CoverLetter coverLetter) {
        return service.createCoverLetter(coverLetter);
    }
}
