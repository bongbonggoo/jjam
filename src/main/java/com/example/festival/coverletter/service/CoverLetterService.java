package com.example.festival.coverletter.service;

import com.example.festival.coverletter.entity.CoverLetter;
import com.example.festival.coverletter.repository.CoverLetterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoverLetterService {

    private final CoverLetterRepository repository;

    public CoverLetterService(CoverLetterRepository repository) {
        this.repository = repository;
    }

    public List<CoverLetter> getAllCoverLetters() {
        return repository.findAll();
    }

    public CoverLetter getCoverLetter(Long id) {
        return repository.findById(id).orElse(null);
    }

    public CoverLetter createCoverLetter(CoverLetter coverLetter) {
        return repository.save(coverLetter);
    }
}
