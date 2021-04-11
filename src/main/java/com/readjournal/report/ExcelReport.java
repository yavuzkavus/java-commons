package com.readjournal.report;

import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;

import com.readjournal.util.IntStr;
import com.readjournal.util.StringUtil;
import com.readjournal.util.Utils;

public class ExcelReport {
	private List<ExcelSheet> sheets;

	public ExcelReport() {
	}

	public ExcelReport(List<ExcelSheet> sheets) {
		this.sheets = sheets;
	}

	public List<ExcelSheet> getSheets() {
		return sheets;
	}

	public void setSheets(List<ExcelSheet> sheets) {
		this.sheets = sheets;
	}

	public void createSheet(String name) {
		if( sheets==null )
			sheets = new ArrayList<>(2);
		sheets.add( new ExcelSheet(name, new ArrayList<ExcelSheetPart>(2)) );
	}

	public void createSheetPart(Object[] elements, String[] fields) {
		createSheetPart(null, null, elements, fields);
	}

	public <T> void createSheetPart(Object[] elements, Function<T, Object[]> mapper) {
		createSheetPart(null, null, elements, mapper);
	}

	public void createSheetPart(String title, String[] labels, Object[] elements, String[] fields) {
		createSheetPart(title, labels, false, elements, fields);
	}

	public <T> void createSheetPart(String title, String[] labels, Object[] elements, Function<T, Object[]> mapper) {
		createSheetPart(title, labels, false, elements, mapper);
	}

	public void createSheetPart(String title, String[] labels, boolean fixedLabels, Object[] elements, String[] fields) {
		createSheetPart(title, labels, fixedLabels, elements, toFunction(elements, fields));
	}

	public <T> void createSheetPart(String title, String[] labels, boolean fixedLabels, Object[] elements, Function<T, Object[]> mapper) {
		if( sheets.isEmpty() )
			throw new RuntimeException("no sheets added!");
		ExcelSheet sheet = sheets.get(sheets.size()-1);
		sheet.createSheetPart(title, labels, fixedLabels, elements, Utils.cast(mapper));
	}

	public void saveReport(File outFile,short foregroundColor,short titleColor) {
		try(HSSFWorkbook workbook = new HSSFWorkbook()) {
			HSSFFont titleFont = workbook.createFont();
			titleFont.setColor(titleColor);
			titleFont.setBold(true);
			titleFont.setFontName(HSSFFont.FONT_ARIAL);

			HSSFCellStyle titleStyle = workbook.createCellStyle();
			titleStyle.setFillForegroundColor(foregroundColor);
			titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			titleStyle.setFillPattern(FillPatternType.BIG_SPOTS);
			titleStyle.setFont(titleFont);
			titleStyle.setBorderRight(BorderStyle.THIN);
			titleStyle.setBorderBottom(BorderStyle.THIN);

			HSSFCellStyle labelStyle = workbook.createCellStyle();
			labelStyle.cloneStyleFrom(titleStyle);


			titleStyle.setAlignment(HorizontalAlignment.CENTER);
			titleStyle.setBottomBorderColor(IndexedColors.WHITE.index);
			labelStyle.setRightBorderColor(IndexedColors.WHITE.index);
			titleStyle.setWrapText(true);

			HSSFCellStyle dateFormatStyle = workbook.createCellStyle();
			dateFormatStyle.setDataFormat(workbook.createDataFormat().getFormat("d MMMM yyyy"));

			for(ExcelSheet excelSheet : sheets) {
				int rowInd = 0;
				Sheet sheet = workbook.createSheet(excelSheet.getName().replaceAll("[\\*\\\\/\\?]", "_"));
				if( excelSheet.getSheetParts()!=null && !excelSheet.getSheetParts().isEmpty() ) {
					for(ExcelSheetPart excelSheetPart : excelSheet.getSheetParts()) {
						boolean hasTitle = !StringUtil.empty(excelSheetPart.getTitle());
						String[] labels = excelSheetPart.getLabels();
						if( hasTitle && labels!=null && labels.length>0 ) {
							if( rowInd!=0 )
								rowInd++;
							Row row = sheet.createRow(rowInd);
							row.setHeightInPoints((1 + excelSheetPart.getTitle().split("[\\r\\n]+").length)*sheet.getDefaultRowHeightInPoints());
							Cell cell = row.createCell(0);
							cell.setCellStyle(titleStyle);
							cell.setCellValue(excelSheetPart.getTitle());
							sheet.addMergedRegion(new CellRangeAddress(rowInd, rowInd, 0, labels.length-1));
							rowInd++;
						}

						//insert labels
						if( excelSheetPart.getLabels()!=null ) {
							if( !hasTitle && rowInd!=0 )
								rowInd++;
							Row row = sheet.createRow(rowInd);
							row.setHeightInPoints(3*sheet.getDefaultRowHeightInPoints()/2);
							int cellInd = 0;
							for(String label : excelSheetPart.getLabels()) {
								Cell cell = row.createCell(cellInd++);
								cell.setCellStyle( labelStyle );
								cell.setCellValue( label );
							}
							if( excelSheetPart.isFixedLabels() ) {
								sheet.createFreezePane(0, rowInd+1);
							}
							rowInd++;
						}

						//insert elements
						Object[] elements = excelSheetPart.getElements();
						if( elements!=null && elements.length>0 ) {
							for(Object element : elements) {
								Row row = sheet.createRow(rowInd++);
								int cellInd = 0;
								Object[] values = excelSheetPart.mapper.apply(element);
								for(Object value : values) {
									Cell cell = row.createCell(cellInd++);
									if( value==null )
										cell.setCellValue("");
									else if( value instanceof Date ) {
										cell.setCellStyle(dateFormatStyle);
										cell.setCellValue((Date)value);
									}
									else if( value instanceof Calendar )
										cell.setCellValue((Calendar)value);
									else if( value instanceof Number )
										cell.setCellValue(((Number)value).doubleValue());
									else if( value instanceof Boolean )
										cell.setCellValue((Boolean)value);
									else
										cell.setCellValue(value.toString());
								}
							}

							for(int i=0; i<elements.length; i++) {
								sheet.autoSizeColumn(i, true);
							}
						}
					}
				}
			}
			try(FileOutputStream fos = new FileOutputStream(outFile)) {
				workbook.write(fos);
			}
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}


	public void saveReport(File outFile) {
		saveReport(outFile, IndexedColors.LIME.index, IndexedColors.WHITE.index);
	}

	private static Function<Object, Object[]> toFunction(Object[] elements, String[] fields) {
		if( elements!=null && elements.length>0 ) {
			Method allMethods[] = elements[0].getClass().getMethods();
			Method[] getMethods = new Method[fields.length];
			for(int i=0; i<fields.length; i++) {
				Method method = null;
				for(Method m : allMethods) {
					String name = m.getName();
					if( name.startsWith("get") && name.length()>3 ||
							name.startsWith("is") && name.length()>2 ) {
						name = name.substring(name.startsWith("get") ? 3 : 2);
						name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
						if( name.equals(fields[i]) ) {
							method = m;
							break;
						}
					}
				}

				if( method==null ) {
					throw new RuntimeException("field " + fields[i] + " not found");
				}
				getMethods[i] = method;
			}
			return o-> {
				Object[] rets = new Object[getMethods.length];
				int i = 0;
				for(Method getMethod : getMethods) {
					try {
						rets[i++] = getMethod.invoke(o);
					}
					catch(Exception e) {
						throw Utils.runtime(e);
					}
				}
				return rets;
			};
		}
		return null;
	}

	class ExcelSheet {
		private String name;
		private List<ExcelSheetPart> sheetParts;

		public ExcelSheet(String name, List<ExcelSheetPart> sheetParts) {
			this.name = name;
			this.sheetParts = sheetParts;
		}

		public void createSheetPart(String title, String[] labels, Object[] elements, Function<Object, Object[]> mapper) {
			createSheetPart(title, labels, false, elements, mapper);
		}

		public void createSheetPart(String title, String[] labels, boolean fixedLabels, Object[] elements, Function<Object, Object[]> mapper) {
			ExcelSheetPart sheetPart = new ExcelSheetPart(title, labels, fixedLabels, elements, mapper);
			if( sheetParts==null ) {
				sheetParts = new ArrayList<>(1);
			}
			sheetParts.add(sheetPart);
		}

		public void createSheetPart(Object[] elements, Function<Object, Object[]> mapper) {
			createSheetPart(null, null, elements, mapper);
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setSheetParts(List<ExcelSheetPart> sheetParts) {
			this.sheetParts = sheetParts;
		}

		public List<ExcelSheetPart> getSheetParts() {
			return sheetParts;
		}
	}

	class ExcelSheetPart {
		String title;
		boolean fixedLabels;
		String labels[];
		Object elements[];
		Function<Object, Object[]> mapper;
		int colCount;

		public ExcelSheetPart(Object[] elements, String[] fields, Function<Object, Object[]> mapper) {
			this(null, null, elements, mapper);
		}

		public ExcelSheetPart(String title, String[] labels, Object[] elements, Function<Object, Object[]> mapper) {
			this(title, labels, false, elements, mapper);
		}

		public ExcelSheetPart(String title, String[] labels, boolean fixedLabels, Object[] elements, Function<Object, Object[]> mapper) {
			this.title = title;
			this.fixedLabels = fixedLabels;
			this.labels = labels;
			this.elements = elements;
			this.mapper = mapper;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public String[] getLabels() {
			return labels;
		}

		public Object[] getElements() {
			return elements;
		}

		public boolean isFixedLabels() {
			return fixedLabels;
		}
	}

	public static void main(String[] args) throws Exception {
		ExcelReport report = new ExcelReport();
		report.createSheet("First Sheet");
		IntStr elements[] = {
			new IntStr(1, "bir"),
			new IntStr(2, "iki"),
			new IntStr(3, "üç"),
			new IntStr(4, "dört"),
			new IntStr(5, "beş"),
			new IntStr(6, "altı"),
			new IntStr(7, "yedi"),
			new IntStr(8, "sekiz"),
			new IntStr(9, "dokuz"),
			new IntStr(10, "on")
		};
		report.createSheetPart(elements, new String[] {"int", "str"} );

		IntStr columnists[] = {
				new IntStr(1, "Murat Murat Murat"),
				new IntStr(2, "Halit"),
				new IntStr(3, "Şerif")
		};
		report.createSheetPart("Yayıncılar", new String[] {"Integer", "String"}, columnists, new String[] {"int", "str"} );

		report.createSheet("Second Sheet");

		Rectangle rects[] = {
			new Rectangle(0, 0, 400, 200),
			new Rectangle(10, 10, 400, 300),
			new Rectangle(20, 20, 300, 400),
			new Rectangle(30, 30, 200, 200),
			new Rectangle(40, 40, 400, 400)
		};
		report.createSheetPart("Dikdörtgenler", new String[] {"x", "y", "w", "h"}, true, rects, new String[] {"x", "y", "width", "height"} );

		Point2D points[] = {
			new Point2D.Double(40.3, 20.0),
			new Point2D.Double(41.7, 30.5),
			new Point2D.Double(12.5, 20.5),
			new Point2D.Double(10.1, 30.0),
			new Point2D.Double(20.2, 30.3)
		};
		report.createSheetPart("Noktalar", new String[] {"x", "y"}, true, points, new String[] {"x", "y"} );

		File file = new File("C:/users/yavuz/desktop/excel.xls");
		report.saveReport(file);

		Desktop.getDesktop().browse(file.toURI());
	}


}
