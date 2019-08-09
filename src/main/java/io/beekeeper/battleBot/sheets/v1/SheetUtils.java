package io.beekeeper.battleBot.sheets.v1;

import java.util.Arrays;
import java.util.List;

import com.google.api.services.sheets.v4.model.BasicChartAxis;
import com.google.api.services.sheets.v4.model.BasicChartDomain;
import com.google.api.services.sheets.v4.model.BasicChartSeries;
import com.google.api.services.sheets.v4.model.BasicChartSpec;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.ChartData;
import com.google.api.services.sheets.v4.model.ChartSourceRange;
import com.google.api.services.sheets.v4.model.ChartSpec;
import com.google.api.services.sheets.v4.model.EmbeddedChart;
import com.google.api.services.sheets.v4.model.EmbeddedObjectPosition;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.OverlayPosition;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.TextFormat;

public class SheetUtils {

    public static GridData grid() {
        return new GridData();
    }

    public static List<GridData> grids(GridData... data) {
        return Arrays.asList(data);
    }

    public static GridData grid(int column, int row) {
        return grid().setStartColumn(column).setStartRow(row);
    }

    public static List<RowData> rows(RowData... data) {
        return Arrays.asList(data);
    }

    public static RowData row(CellData... data) {
        return new RowData().setValues(Arrays.asList(data));
    }

    public static CellData cell(String value) {
        return cell(value, null);
    }

    public static CellData cell(Integer value) {
        return cell(new ExtendedValue().setNumberValue(new Double(value)), null);
    }

    public static CellData cell(Long value) {
        return cell(new ExtendedValue().setNumberValue(new Double(value)), null);
    }

    public static CellData cell(String value, CellFormat format) {
        return cell(new ExtendedValue().setStringValue(value), format);
    }

    public static CellData cell(ExtendedValue value, CellFormat format) {
        CellData cell = new CellData().setUserEnteredValue(value);
        if (format != null) {
            cell.setUserEnteredFormat(format);
        }
        return cell;
    }

    public static CellData formula(String value) {
        return formula(value, null);
    }

    public static CellData formula(String value, CellFormat format) {
        CellData cell = new CellData().setUserEnteredValue(new ExtendedValue().setFormulaValue(value));
        if (format != null) {
            cell.setUserEnteredFormat(format);
        }
        return cell;
    }

    public static GridRange range(Integer cStart, Integer cEnd, Integer rStart, Integer rEnd) {
        return new GridRange()
            .setStartColumnIndex(cStart)
            .setEndColumnIndex(cEnd)
            .setStartRowIndex(rStart)
            .setEndRowIndex(rEnd);
    }

    public static class Format {
        public static final CellFormat bold = new CellFormat().setTextFormat(new TextFormat().setBold(true));
    }

    public static class Charts {
        public static final EmbeddedObjectPosition position(Integer x, Integer y, Integer width, Integer height) {
            return new EmbeddedObjectPosition().setOverlayPosition(
                new OverlayPosition().setOffsetXPixels(x)
                    .setOffsetYPixels(y)
                    .setWidthPixels(width)
                    .setHeightPixels(height)
            );
        }

        public static final EmbeddedChart line(String title,
                                               String axisTitle,
                                               GridRange domainRange,
                                               GridRange dataRange) {
            return new EmbeddedChart()
                .setSpec(
                    new ChartSpec().setTitle(title)
                        .setBasicChart(
                            new BasicChartSpec()
                                .setChartType("LINE")
                                .setLegendPosition("BOTTOM_LEGEND")
                                .setAxis(
                                    Arrays.asList(new BasicChartAxis().setTitle(axisTitle).setPosition("BOTTOM_AXIS"))
                                )
                                .setDomains(
                                    Arrays.asList(
                                        new BasicChartDomain().setDomain(
                                            new ChartData().setSourceRange(
                                                new ChartSourceRange().setSources(
                                                    Arrays.asList(
                                                        domainRange
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                                .setSeries(
                                    Arrays.asList(
                                        new BasicChartSeries().setSeries(
                                            new ChartData().setSourceRange(
                                                new ChartSourceRange()
                                                    .setSources(
                                                        Arrays.asList(
                                                            dataRange
                                                        )
                                                    )
                                            )
                                        )
                                    )
                                )
                        )
                );
        }
    }
}
