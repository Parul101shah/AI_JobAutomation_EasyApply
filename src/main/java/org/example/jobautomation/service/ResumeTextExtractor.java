package org.example.jobautomation.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeTextExtractor {
    public String extractText(MultipartFile file) throws IOException {
        try(PDDocument doc =Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
