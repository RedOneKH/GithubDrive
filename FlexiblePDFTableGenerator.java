package com.redone.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlexiblePDFTableGenerator {

    // Cell alignment options
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    // Cell configuration class
    public static class CellStyle {
        private PDFont font = PDType1Font.HELVETICA;
        private float fontSize = 10f;
        private Color textColor = Color.BLACK;
        private Color backgroundColor = null;
        private Alignment alignment = Alignment.LEFT;
        private float padding = 5f;

        public CellStyle font(PDFont font) { this.font = font; return this; }
        public CellStyle fontSize(float fontSize) { this.fontSize = fontSize; return this; }
        public CellStyle textColor(Color textColor) { this.textColor = textColor; return this; }
        public CellStyle backgroundColor(Color backgroundColor) { this.backgroundColor = backgroundColor; return this; }
        public CellStyle alignment(Alignment alignment) { this.alignment = alignment; return this; }
        public CellStyle padding(float padding) { this.padding = padding; return this; }

        // Getters
        public PDFont getFont() { return font; }
        public float getFontSize() { return fontSize; }
        public Color getTextColor() { return textColor; }
        public Color getBackgroundColor() { return backgroundColor; }
        public Alignment getAlignment() { return alignment; }
        public float getPadding() { return padding; }
    }

    // Table cell class
    public static class Cell {
        private String content;
        private CellStyle style;

        public Cell(String content) {
            this.content = content;
            this.style = new CellStyle();
        }

        public Cell(String content, CellStyle style) {
            this.content = content;
            this.style = style;
        }

        public String getContent() { return content; }
        public CellStyle getStyle() { return style; }
    }

    // Table class
    public static class Table {
        private List<List<Cell>> rows;
        private float[] columnWidths;
        private float rowHeight = 20f;
        private float borderWidth = 1f;
        private Color borderColor = Color.BLACK;

        public Table(int columns) {
            this.rows = new ArrayList<>();
            this.columnWidths = new float[columns];
            // Default equal column widths
            for (int i = 0; i < columns; i++) {
                columnWidths[i] = 100f;
            }
        }

        public Table columnWidths(float... widths) {
            this.columnWidths = widths;
            return this;
        }

        public Table rowHeight(float rowHeight) {
            this.rowHeight = rowHeight;
            return this;
        }

        public Table borderWidth(float borderWidth) {
            this.borderWidth = borderWidth;
            return this;
        }

        public Table borderColor(Color borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public void addRow(Cell... cells) {
            List<Cell> row = new ArrayList<>();
            for (Cell cell : cells) {
                row.add(cell);
            }
            rows.add(row);
        }

        public List<List<Cell>> getRows() { return rows; }
        public float[] getColumnWidths() { return columnWidths; }
        public float getRowHeight() { return rowHeight; }
        public float getBorderWidth() { return borderWidth; }
        public Color getBorderColor() { return borderColor; }
    }

    // IndexPageBuilder class to manage page state and rendering
    public static class IndexPageBuilder {
        private PDDocument document;
        private PDPage currentPage;
        private List<PDPage> pages;
        private PDPageContentStream currentContentStream;
        private float currentY;
        private float topMargin = 50f;
        private float bottomMargin = 50f;
        private float leftMargin = 50f;
        private float rightMargin = 50f;
        private String pageHeaderText;
        private PDFont headerFont = PDType1Font.HELVETICA_BOLD;
        private float headerFontSize = 16f;
        private Color headerColor = Color.BLACK;

        public IndexPageBuilder(){
            this.document = new PDDocument();
            this.pages = new ArrayList<>();
        }

        public IndexPageBuilder build() throws IOException {
            initializeFirstPage();
            return this;
        }

        // Builder pattern setters
        public IndexPageBuilder topMargin(float topMargin) {
            this.topMargin = topMargin;
            return this;
        }

        public IndexPageBuilder bottomMargin(float bottomMargin) {
            this.bottomMargin = bottomMargin;
            return this;
        }

        public IndexPageBuilder leftMargin(float leftMargin) {
            this.leftMargin = leftMargin;
            return this;
        }

        public IndexPageBuilder rightMargin(float rightMargin) {
            this.rightMargin = rightMargin;
            return this;
        }

        public IndexPageBuilder pageHeader(String headerText) {
            this.pageHeaderText = headerText;
            return this;
        }

        public IndexPageBuilder headerFont(PDFont font, float fontSize, Color color) {
            this.headerFont = font;
            this.headerFontSize = fontSize;
            this.headerColor = color;
            return this;
        }

        private void initializeFirstPage() throws IOException {
            currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            pages.add(currentPage);

            currentContentStream = new PDPageContentStream(document, currentPage);
            currentY = currentPage.getMediaBox().getHeight() - topMargin;

            if (!StringUtils.isEmpty(pageHeaderText)) {
                drawPageHeader();
            }
        }

        public void createNewPage() throws IOException {
            if (currentContentStream != null) {
                currentContentStream.close();
            }

            currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            pages.add(0,currentPage);
            currentContentStream = new PDPageContentStream(document, currentPage);
            currentY = currentPage.getMediaBox().getHeight() - topMargin;
        }

        private void drawPageHeader() throws IOException {
            if (pageHeaderText == null || pageHeaderText.isEmpty()) {
                return;
            }

            currentContentStream.beginText();
            currentContentStream.setFont(headerFont, headerFontSize);
            currentContentStream.setNonStrokingColor(headerColor);

            float pageWidth = currentPage.getMediaBox().getWidth();
            float headerWidth = headerFont.getStringWidth(pageHeaderText) / 1000 * headerFontSize;
            float headerX = (pageWidth - headerWidth) / 2;
            float headerY = currentPage.getMediaBox().getHeight() - (topMargin / 2);

            currentContentStream.newLineAtOffset(headerX, headerY);
            currentContentStream.showText(pageHeaderText);
            currentContentStream.endText();

            currentY -= headerFontSize + 10f;
        }

        public void drawTitle(String title, PDFont font, float fontSize, Color color) throws IOException {
            if (title == null || title.isEmpty()) {
                return;
            }

            currentContentStream.beginText();
            currentContentStream.setFont(font, fontSize);
            currentContentStream.setNonStrokingColor(color);
            currentContentStream.newLineAtOffset(leftMargin, currentY);
            currentContentStream.showText(title);
            currentContentStream.endText();

            currentY -= fontSize + 10f;
        }

        public void drawTitle(String title) throws IOException {
            drawTitle(title, PDType1Font.HELVETICA_BOLD, 14f, Color.BLACK);
        }

        public boolean canFitContent(float requiredHeight) {
            return (currentY - requiredHeight) >= bottomMargin;
        }

        public void moveY(float offset) {
            currentY += offset;
        }

        public void close() throws IOException {
            if (currentContentStream != null) {
                currentContentStream.close();
            }
        }

        // Table drawing methods
        public void drawTable(Table table, String tableTitle) throws IOException {
            float totalWidth = 0;
            for (float width : table.getColumnWidths()) {
                totalWidth += width;
            }

            // Check if we can fit the table start (title + header + one row) on current page
            if (!canFitTableStart(table, tableTitle)) {
                createNewPage();
            }

            // Draw table title
            if (tableTitle != null && !tableTitle.isEmpty()) {
                drawTitle(tableTitle);
            }

            // Draw each row
            for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
                List<Cell> row = table.getRows().get(rowIndex);

                // Calculate actual row height needed for this row (considering text wrapping)
                float actualRowHeight = calculateRowHeight(row, table.getColumnWidths(),
                        table.getRowHeight(), 5f);

                // Check if we need a new page
                if (currentY - actualRowHeight < bottomMargin) {
                    createNewPage();

                    // Redraw table title on new page
                    if (tableTitle != null && !tableTitle.isEmpty()) {
                        drawTitle(tableTitle);
                    }

                    // If this is not the first row, redraw the header row
                    if (rowIndex > 0 && !table.getRows().isEmpty()) {
                        List<Cell> headerRow = table.getRows().get(0);
                        float headerRowHeight = calculateRowHeight(headerRow, table.getColumnWidths(),
                                table.getRowHeight(), 5f);

                        // Draw header row
                        drawSingleRow(headerRow, table, headerRowHeight, totalWidth);
                        currentY -= headerRowHeight;
                    }
                }

                // Draw the current row
                drawSingleRow(row, table, actualRowHeight, totalWidth);
                currentY -= actualRowHeight;
            }
        }

        private boolean canFitTableStart(Table table, String tableTitle) throws IOException {
            float requiredSpace = 0;

            // Space for table title
            if (tableTitle != null && !tableTitle.isEmpty()) {
                requiredSpace += 25f; // Title height + spacing
            }

            // Space for table header (first row) if it exists
            if (!table.getRows().isEmpty()) {
                List<Cell> headerRow = table.getRows().get(0);
                float headerHeight = calculateRowHeight(headerRow, table.getColumnWidths(), table.getRowHeight(), 5f);
                requiredSpace += headerHeight;
            }

            // Space for at least one data row if it exists
            if (table.getRows().size() > 1) {
                List<Cell> firstDataRow = table.getRows().get(1);
                float dataRowHeight = calculateRowHeight(firstDataRow, table.getColumnWidths(), table.getRowHeight(), 5f);
                requiredSpace += dataRowHeight;
            }

            return canFitContent(requiredSpace);
        }

        private void drawSingleRow(List<Cell> row, Table table, float actualRowHeight, float totalWidth) throws IOException {
            float currentX = leftMargin;

            // Draw background colors first
            for (int i = 0; i < row.size() && i < table.getColumnWidths().length; i++) {
                Cell cell = row.get(i);
                float cellWidth = table.getColumnWidths()[i];

                if (cell.getStyle().getBackgroundColor() != null) {
                    currentContentStream.setNonStrokingColor(cell.getStyle().getBackgroundColor());
                    currentContentStream.addRect(currentX, currentY - actualRowHeight,
                            cellWidth, actualRowHeight);
                    currentContentStream.fill();
                }
                currentX += cellWidth;
            }

            // Draw borders
            currentContentStream.setStrokingColor(table.getBorderColor());
            currentContentStream.setLineWidth(table.getBorderWidth());

            // Horizontal lines
            currentContentStream.moveTo(leftMargin, currentY);
            currentContentStream.lineTo(leftMargin + totalWidth, currentY);
            currentContentStream.stroke();

            currentContentStream.moveTo(leftMargin, currentY - actualRowHeight);
            currentContentStream.lineTo(leftMargin + totalWidth, currentY - actualRowHeight);
            currentContentStream.stroke();

            // Vertical lines
            float x = leftMargin;
            for (float width : table.getColumnWidths()) {
                currentContentStream.moveTo(x, currentY);
                currentContentStream.lineTo(x, currentY - actualRowHeight);
                currentContentStream.stroke();
                x += width;
            }
            // Last vertical line
            currentContentStream.moveTo(leftMargin + totalWidth, currentY);
            currentContentStream.lineTo(leftMargin + totalWidth, currentY - actualRowHeight);
            currentContentStream.stroke();

            // Draw text content with wrapping
            currentX = leftMargin;
            for (int i = 0; i < row.size() && i < table.getColumnWidths().length; i++) {
                Cell cell = row.get(i);
                float cellWidth = table.getColumnWidths()[i];
                CellStyle style = cell.getStyle();

                if (!StringUtils.isEmpty(cell.getContent())) {
                    float availableWidth = cellWidth - (2 * style.getPadding());
                    String content = isURL(cell.getContent()) ? "Link" : cell.getContent();
                    List<String> lines = wrapText(content, style.getFont(),
                            style.getFontSize(), availableWidth);

                    Color textColor = isURL(cell.content) ? Color.BLUE : style.getTextColor();
                    currentContentStream.setFont(style.getFont(), style.getFontSize());
                    currentContentStream.setNonStrokingColor(textColor);

                    // Calculate starting Y position for multi-line text (center vertically)
                    float totalTextHeight = lines.size() * (style.getFontSize() + 2);
                    float startTextY = currentY - (actualRowHeight - totalTextHeight) / 2 - style.getFontSize();

                    for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                        String line = lines.get(lineIndex);

                        currentContentStream.beginText();

                        // Calculate text position based on alignment
                        float textWidth = style.getFont().getStringWidth(line) / 1000 * style.getFontSize();
                        float textX;

                        switch (style.getAlignment()) {
                            case CENTER:
                                textX = currentX + (cellWidth - textWidth) / 2;
                                break;
                            case RIGHT:
                                textX = currentX + cellWidth - textWidth - style.getPadding();
                                break;
                            case LEFT:
                            default:
                                textX = currentX + style.getPadding();
                                break;
                        }

                        float textY = startTextY - (lineIndex * (style.getFontSize() + 2));
                        currentContentStream.newLineAtOffset(textX, textY);
                        currentContentStream.showText(line);
                        currentContentStream.endText();

                        if (isURL(cell.getContent())) {
                            createLinkAnnotation(cell.getContent().trim(),
                                    textX, textY + style.getFontSize(),
                                    textWidth, style.getFontSize());
                        }
                    }
                }

                currentX += cellWidth;
            }
        }

        // Helper method to wrap text within a given width with support for snake_case and camelCase
        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                return lines;
            }

            // Split text into tokens that can be wrapped at natural breaking points
            List<String> tokens = splitTextIntoTokens(text);
            StringBuilder currentLine = new StringBuilder();

            for (String token : tokens) {
                String testLine = currentLine.length() == 0 ? token : currentLine + token;
                float textWidth = font.getStringWidth(testLine) / 1000 * fontSize;

                if (textWidth > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(token);
                } else {
                    currentLine.append(token);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            return lines;
        }

        // Helper method to split text into tokens for wrapping at spaces, snake_case, and camelCase
        private List<String> splitTextIntoTokens(String text) {
            List<String> tokens = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                return tokens;
            }

            StringBuilder currentToken = new StringBuilder();
            char[] chars = text.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                char currentChar = chars[i];

                currentToken.append(currentChar);

                // Check for breaking points
                boolean shouldBreak = false;

                // Break at spaces
                if (currentChar == ' ') {
                    shouldBreak = true;
                }
                // Break at underscores (snake_case) - keep underscore with previous part
                else if (currentChar == '_') {
                    shouldBreak = true;
                }
                // Break at camelCase transitions (lowercase to uppercase)
                else if (i > 0 && Character.isLowerCase(chars[i - 1]) && Character.isUpperCase(currentChar)) {
                    // Move the uppercase character to the next token
                    currentToken.setLength(currentToken.length() - 1);
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                    }
                    currentToken = new StringBuilder();
                    currentToken.append(currentChar);
                }
                // Break before numbers if preceded by letters
                else if (i > 0 && Character.isLetter(chars[i - 1]) && Character.isDigit(currentChar)) {
                    // Move the digit to the next token
                    currentToken.setLength(currentToken.length() - 1);
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                    }
                    currentToken = new StringBuilder();
                    currentToken.append(currentChar);
                }
                // Break after numbers if followed by letters
                else if (i > 0 && Character.isDigit(chars[i - 1]) && Character.isLetter(currentChar)) {
                    // Move the letter to the next token
                    currentToken.setLength(currentToken.length() - 1);
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                    }
                    currentToken = new StringBuilder();
                    currentToken.append(currentChar);
                }

                // Add token when we hit a breaking point or reach the end
                if (shouldBreak || i == chars.length - 1) {
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken = new StringBuilder();
                    }
                }
            }

            return tokens;
        }
        // Helper method to calculate required row height for wrapped text
        private float calculateRowHeight(List<Cell> row, float[] columnWidths, float defaultRowHeight, float padding) throws IOException {
            float maxHeight = defaultRowHeight;

            for (int i = 0; i < row.size() && i < columnWidths.length; i++) {
                Cell cell = row.get(i);
                if (cell.getContent() != null && !cell.getContent().isEmpty()) {
                    float availableWidth = columnWidths[i] - (2 * padding);
                    List<String> lines = wrapText(cell.getContent(), cell.getStyle().getFont(),
                            cell.getStyle().getFontSize(), availableWidth);

                    float requiredHeight = lines.size() * (cell.getStyle().getFontSize() + 2) + (2 * padding);
                    maxHeight = Math.max(maxHeight, requiredHeight);
                }
            }

            return maxHeight;
        }


        private static boolean isURL(String text) {
            if (text == null || text.trim().isEmpty()) {
                return false;
            }
            return text.contains("http");
        }
        // Helper method to create clickable link annotation
        private void createLinkAnnotation(String url, float x, float y, float width, float height) throws IOException {
            PDAnnotationLink link = new PDAnnotationLink();

            // Set the rectangle for the clickable area
            PDRectangle rect = new PDRectangle(x, y - height, width, height);
            link.setRectangle(rect);

            // Set the URI action
            PDActionURI action = new PDActionURI();
            action.setURI(url);
            link.setAction(action);

            // Make the link invisible (no border)
            link.setBorderStyle(null);

            // Add the annotation to the page
            currentPage.getAnnotations().add(link);
        }


        public List<PDPage> drawIndexPage(Document doc) throws IOException{

            drawInfoTable(doc.getDocumentInfo());

            // Add some spacing
            moveY(-30f);

            drawTagsTable(doc.getPageTags());

            // Close the page builder
            close();

            // Save the document
            document.save("flexible_tables_refactored.pdf");
            document.close();

            return pages;
        }

        public void drawInfoTable(Map<String, String> documentInfo) throws IOException {

            CellStyle grayHeaderStyle = new CellStyle()
                    .backgroundColor(new Color(138, 138, 141, 255))
                    .font(PDType1Font.HELVETICA_BOLD)
                    .alignment(Alignment.LEFT);

            // Create a table
            Table table = new Table(2)
                    .columnWidths(200f, 300f)
                    .rowHeight(10f)
                    .borderWidth(1.5f)
                    .borderColor(Color.DARK_GRAY);

            CellStyle leftAlignedStyle = new CellStyle()
                    .alignment(Alignment.LEFT)
                    .backgroundColor(new Color(245, 245, 245));



            for (var docInfo: documentInfo.entrySet()) {
                table.addRow(
                        new Cell(docInfo.getKey(), grayHeaderStyle),
                        new Cell(docInfo.getValue(), leftAlignedStyle)
                );
            }

            // Draw the first table
            drawTable(table, "Document informations");
        }

        public void drawTagsTable(List<PageTags> pageTags) throws IOException {

            CellStyle blueHeaderStyle = new CellStyle()
                    .backgroundColor(new Color(100, 150, 200))
                    .textColor(Color.WHITE)
                    .font(PDType1Font.HELVETICA_BOLD)
                    .alignment(Alignment.CENTER);

            CellStyle centeredStyle = new CellStyle()
                    .alignment(Alignment.CENTER)
                    .backgroundColor(new Color(245, 245, 245));

            // Create tags table
            Table tagsTable = new Table(6)
                    .columnWidths(100,100,30,70,100,100)
                    .rowHeight(15f);

            tagsTable.addRow(
                    new Cell("Group", blueHeaderStyle),
                    new Cell("Type", blueHeaderStyle),
                    new Cell("Page", blueHeaderStyle),
                    new Cell("status", blueHeaderStyle),
                    new Cell("Comment", blueHeaderStyle),
                    new Cell("Comment Info", blueHeaderStyle)
            );

            for (var tag : pageTags.stream().flatMap(pt -> pt.getTags().stream()).collect(Collectors.toList())) {
                tagsTable.addRow(
                        new Cell(tag.getGroups()),
                        new Cell(tag.getType()),
                        new Cell(""+tag.getPage(), centeredStyle),
                        new Cell(tag.getStatus()),
                        new Cell(tag.getComment()),
                        new Cell(tag.getCommentInfo())
                );
            }

            // Draw tags table
            drawTable(tagsTable, "Extracted tags");
        }
    }


    public static void main(String[] args) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Document doc = objectMapper.readValue(new File("input.json"),
                    new TypeReference<>() {
                    });


            // Create IndexPageBuilder with custom margins and header
            IndexPageBuilder pageBuilder = new IndexPageBuilder()
                    .topMargin(70f)
                    .bottomMargin(50f)
                    .leftMargin(50f)
                    .rightMargin(50f)
                    .pageHeader("INDEX")
                    .headerFont(PDType1Font.HELVETICA_BOLD, 16f, Color.BLACK)
                    .build();

            var pages = pageBuilder.drawIndexPage(doc);
            System.out.println(pages);

            System.out.println("PDF with flexible tables created successfully using IndexPageBuilder!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}