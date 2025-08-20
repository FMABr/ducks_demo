package com.pjusto.ducks.reporting;

import com.pjusto.ducks.duck.Duck;
import com.pjusto.ducks.duck.DuckRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
public class SoldDuckReportController {
    private final DuckRepository duckRepo;
    private final SoldDuckViewRepository soldViewRepo;

    public SoldDuckReportController(DuckRepository duckRepo, SoldDuckViewRepository soldViewRepo) {
        this.duckRepo = duckRepo;
        this.soldViewRepo = soldViewRepo;
    }

    @GetMapping(value = "/ducks.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> ducksExcel() throws Exception {
        List<Duck> ducks = duckRepo.findAll();

        Map<Long, SoldDuckView> soldByDuckId = soldViewRepo.findAll().stream()
                .collect(Collectors.toMap(SoldDuckView::getDuckId, Function.identity()));

        Map<Long, List<Duck>> children = new HashMap<>();
        List<Duck> roots = new ArrayList<>();
        for (Duck d : ducks) {
            Long momId = (d.getMother() != null) ? d.getMother().getId() : null;
            if (momId == null) {
                roots.add(d);
            } else {
                children.computeIfAbsent(momId, k -> new ArrayList<>()).add(d);
            }
        }
        roots.sort(Comparator.comparing(Duck::getName, String.CASE_INSENSITIVE_ORDER));
        children.values().forEach(list -> list.sort(Comparator.comparing(Duck::getName, String.CASE_INSENSITIVE_ORDER)));

        int maxDepth = 0;
        Deque<Map.Entry<Duck,Integer>> stack = new ArrayDeque<>();
        for (Duck r : roots) stack.push(Map.entry(r, 0));
        while (!stack.isEmpty()) {
            var e = stack.pop();
            maxDepth = Math.max(maxDepth, e.getValue());
            for (Duck ch : children.getOrDefault(e.getKey().getId(), List.of())) {
                stack.push(Map.entry(ch, e.getValue() + 1));
            }
        }
        int nameCols = Math.max(1, maxDepth + 1);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("GERENCIAMENTO DE PATOS");

            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short)14);
            titleStyle.setFont(titleFont); titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont(); headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = wb.createCellStyle();
            DataFormat fmt = wb.createDataFormat();
            currencyStyle.setDataFormat(fmt.getFormat("[$R$-416] #,##0.00"));

            int totalCols = nameCols + 4;
            Row titleRow = sheet.createRow(0);
            for (int c = 0; c < totalCols; c++) titleRow.createCell(c);
            titleRow.getCell(0).setCellValue("GERENCIAMENTO DE PATOS");
            titleRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, totalCols - 1));

            Row header = sheet.createRow(2);
            for (int c = 0; c < totalCols; c++) header.createCell(c);
            header.getCell(0).setCellValue("Nome");
            header.getCell(nameCols).setCellValue("Status");
            header.getCell(nameCols + 1).setCellValue("Cliente");
            header.getCell(nameCols + 2).setCellValue("Tipo do cliente");
            header.getCell(nameCols + 3).setCellValue("Valor");
            for (int c = 0; c < totalCols; c++) header.getCell(c).setCellStyle(headerStyle);

            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, nameCols - 1));

            int rowIdx = 3;
            rowIdx = writeDuckRows(sheet, rowIdx, roots, children, 0, nameCols, soldByDuckId, currencyStyle);

            for (int c = 0; c < totalCols; c++) sheet.autoSizeColumn(c);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            String filename = "gerenciamento_de_patos.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" +
                                    java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(baos.toByteArray());
        }
    }

    private int writeDuckRows(
            Sheet sheet,
            int rowIdx,
            List<Duck> nodes,
            Map<Long, List<Duck>> children,
            int depth,
            int nameCols,
            Map<Long, SoldDuckView> soldByDuckId,
            CellStyle currencyStyle
    ) {
        for (Duck d : nodes) {
            Row r = sheet.createRow(rowIdx);

            int totalCols = nameCols + 4;
            for (int c = 0; c < totalCols; c++) r.createCell(c);

            int startCol = depth;
            int endCol   = nameCols - 1;

            if (startCol > endCol) startCol = endCol;

            r.getCell(startCol).setCellValue(d.getName());
            mergeRowCellsIfSpanAtLeastTwo(sheet, rowIdx, startCol, endCol);

            SoldDuckView sold = soldByDuckId.get(d.getId());
            if (sold == null) {
                r.getCell(nameCols).setCellValue("Disponível");
                r.getCell(nameCols + 1).setCellValue("-");
                r.getCell(nameCols + 2).setCellValue("-");
                r.getCell(nameCols + 3).setCellValue("-");
            } else {
                r.getCell(nameCols).setCellValue("Vendido");
                r.getCell(nameCols + 1).setCellValue(sold.getCustomerName());

                BigDecimal currentPrice = d.getPrice();
                String tipo = (currentPrice != null && sold.getPriceAtSale().compareTo(currentPrice) < 0)
                        ? "com Desconto" : "sem Desconto";
                r.getCell(nameCols + 2).setCellValue(tipo);

                Cell priceCell = r.getCell(nameCols + 3);
                priceCell.setCellValue(sold.getPriceAtSale().doubleValue());
                priceCell.setCellStyle(currencyStyle);
            }

            rowIdx++;

            List<Duck> kids = children.getOrDefault(d.getId(), List.of());
            if (!kids.isEmpty()) {
                rowIdx = writeDuckRows(sheet, rowIdx, kids, children, depth + 1, nameCols, soldByDuckId, currencyStyle);
            }
        }
        return rowIdx;
    }

    private static void mergeRowCellsIfSpanAtLeastTwo(Sheet sheet, int row, int firstCol, int lastCol) {
        if (lastCol - firstCol >= 1) {
            sheet.addMergedRegion(new CellRangeAddress(row, row, firstCol, lastCol));
        }
    }

}
