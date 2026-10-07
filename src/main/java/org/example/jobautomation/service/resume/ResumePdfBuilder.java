package org.example.jobautomation.service.resume;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.example.jobautomation.dto.TailoredResume;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class ResumePdfBuilder {

    private static final Font NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font H = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font B = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font BB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    public byte[] build(TailoredResume r) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph(nz(r.fullName()), NAME));
            doc.add(new Paragraph(join(" | ", r.email(), r.phone(), r.location()), B));
            if (has(r.headline())) doc.add(new Paragraph(r.headline(), BB));

            section(doc, "SUMMARY");
            doc.add(new Paragraph(nz(r.summary()), B));

            if (r.skills() != null && !r.skills().isEmpty()) {
                section(doc, "SKILLS");
                doc.add(new Paragraph(String.join(", ", r.skills()), B));
            }

            if (r.experience() != null && !r.experience().isEmpty()) {
                section(doc, "EXPERIENCE");
                for (var e : r.experience()) {
                    doc.add(new Paragraph(join(" – ", e.title(), e.company(), e.period()), BB));
                    bullets(doc, e.bullets());
                }
            }

            if (r.projects() != null && !r.projects().isEmpty()) {
                section(doc, "PROJECTS");
                for (var p : r.projects()) {
                    doc.add(new Paragraph(join(" | ", p.name(), p.techStack()), BB));
                    bullets(doc, p.bullets());
                }
            }

            if (r.education() != null && !r.education().isEmpty()) {
                section(doc, "EDUCATION");
                for (String s : r.education()) doc.add(new Paragraph(s, B));
            }

            if (r.certifications() != null && !r.certifications().isEmpty()) {
                section(doc, "CERTIFICATIONS");
                for (String s : r.certifications()) doc.add(new Paragraph(s, B));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF generation failed", e);
        }
    }

    private void section(Document d, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, H);
        p.setSpacingBefore(10);
        d.add(p);
    }

    private void bullets(Document d, List<String> items) throws DocumentException {
        if (items == null) return;
        com.lowagie.text.List l = new com.lowagie.text.List(false, 10);
        for (String s : items) l.add(new ListItem(new Phrase(s, B)));
        d.add(l);
    }

    private boolean has(String s) { return s != null && !s.isBlank(); }
    private String nz(String s) { return s == null ? "" : s; }

    private String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (has(p)) { if (sb.length() > 0) sb.append(sep); sb.append(p); }
        }
        return sb.toString();
    }
}
