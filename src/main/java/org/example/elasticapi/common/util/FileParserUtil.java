package org.example.elasticapi.common.util;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileParserUtil {

    public String classpathJsonParser(String fileName) throws IOException {
        String str;
        StringBuilder stringBuilder = new StringBuilder(4000);
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader((new ClassPathResource(fileName)).getInputStream())
        )) {
            while ((str = bufferedReader.readLine()) != null) {
                stringBuilder.append(str);
            }
            return stringBuilder.toString();
        }
    }

    public String parseFile(File file) {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("파일이 존재하지 않거나 파일이 아닙니다: " + file.getAbsolutePath());
        }

        String lowerName = file.getName().toLowerCase();

        try {
            if (lowerName.endsWith(".hwp")) {
                return hwpFileParser(file);
            } else if (lowerName.endsWith(".hwpx")) {
                return hwpxFileParser(file);
            } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
                return excelFileParser(file);
            } else if (lowerName.endsWith(".pdf")) {
                return pdfFileParser(file);
            } else if (lowerName.endsWith(".csv")) {
                return csvFileParser(file);
            } else if (lowerName.endsWith(".txt")) {
                return textFileParser(file);
            } else if (lowerName.endsWith(".doc")) {
                return parseDoc(file);
            } else if (lowerName.endsWith(".docx")) {
                return parseDocx(file);
            } else if (lowerName.endsWith(".ppt")) {
                return parsePpt(file);
            } else if (lowerName.endsWith(".pptx")) {
                return parsePptx(file);
            } else {
                throw new UnsupportedOperationException("지원하지 않는 파일 형식입니다: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new RuntimeException("파일 파싱 중 오류 발생: " + file.getAbsolutePath(), e);
        }
    }

    private String parseDoc(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String parseDocx(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String csvFileParser(File file) throws IOException {
        StringBuilder result = new StringBuilder();

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {

            for (CSVRecord record : parser) {
                for (String cell : record) {
                    result.append(cell).append(" ");
                }
                result.append("\n");
            }
        }

        return result.toString();
    }

    private String textFileParser(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String pdfFileParser(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String excelFileParser(File file) throws IOException {
        StringBuilder result = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        result.append(cell.toString()).append(" ");
                    }
                    result.append("\n");
                }
            }
        }

        return result.toString();
    }

    private String hwpxFileParser(File file) throws Exception {
        HWPXFile hwpxFile = HWPXReader.fromFile(file);
        if (hwpxFile == null) {
            throw new IllegalStateException("Failed to parse HWPX file: null object returned.");
        }

        TextMarks textMarks = new TextMarks();
        textMarks.paraSeparator("\n");
        textMarks.lineBreak("");

        String extractedText = kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor.extract(
                hwpxFile,
                kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod.InsertControlTextBetweenParagraphText,
                true, // Include control text
                textMarks // Include structured text formats
        );
        log.info("Successfully extracted text content from HWPX.");

        return extractedText;
    }

    private String hwpFileParser(File file) throws Exception {
        HWPFile hwpFile = HWPReader.fromFile(file);

        if (hwpFile == null) {
            throw new IllegalArgumentException("HWP 파일을 읽을 수 없습니다.");
        }
        return kr.dogfoot.hwplib.tool.textextractor.TextExtractor.extract(hwpFile, kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod.InsertControlTextBetweenParagraphText);
    }

    private String parsePpt(File file) throws IOException {
        StringBuilder result = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
             HSLFSlideShow ppt = new HSLFSlideShow(fis)) {

            for (HSLFSlide slide : ppt.getSlides()) {
                // Shape들을 리스트에 모으기
                List<HSLFShape> shapes = new ArrayList<>(slide.getShapes());

                // Y 좌표(상단 위치) 기준으로 오름차순 정렬
                shapes.sort(Comparator.comparingInt(shape -> (int) shape.getAnchor().getY()));

                // 정렬된 순서대로 처리
                for (HSLFShape shape : shapes) {
                    if (shape instanceof HSLFTable) {
                        HSLFTable table = (HSLFTable) shape;
                        int rows = table.getNumberOfRows();
                        int cols = table.getNumberOfColumns();
                        for (int r = 0; r < rows; r++) {
                            for (int c = 0; c < cols; c++) {
                                HSLFTableCell cell = table.getCell(r, c);
                                if (cell != null) {
                                    String cellText = cell.getText();
                                    if (cellText != null && !cellText.isEmpty()) {
                                        result.append(cellText).append("\t");
                                    }
                                }
                            }
                            result.append("\n");
                        }
                    } else if (shape instanceof HSLFTextShape) {
                        HSLFTextShape textShape = (HSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.isEmpty()) {
                            result.append(text).append("\n");
                        }
                    }
                }
                result.append("\n");
            }
        }

        return result.toString();
    }

    private String parsePptx(File file) throws IOException {
        StringBuilder result = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow pptx = new XMLSlideShow(fis)) {

            for (XSLFSlide slide : pptx.getSlides()) {
                List<XSLFShape> shapes = new ArrayList<>(slide.getShapes());

                // Y 좌표(위치) 기준으로 오름차순 정렬
                shapes.sort(Comparator.comparingInt(shape -> {
                    if (shape.getAnchor() != null) {
                        return (int) shape.getAnchor().getY();
                    }
                    return 0;
                }));

                for (XSLFShape shape : shapes) {
                    if (shape instanceof XSLFTable) {
                        XSLFTable table = (XSLFTable) shape;
                        for (XSLFTableRow row : table.getRows()) {
                            for (XSLFTableCell cell : row.getCells()) {
                                String cellText = cell.getText();
                                if (cellText != null && !cellText.isEmpty()) {
                                    result.append(cellText).append("\t");
                                }
                            }
                            result.append("\n");
                        }
                    } else if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.isEmpty()) {
                            result.append(text).append("\n");
                        }
                    }
                }
                result.append("\n");
            }
        }

        return result.toString();
    }
}

