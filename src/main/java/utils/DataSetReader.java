package utils;

import entities.DataRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
@Slf4j
public class DataSetReader {
    public static List<DataRecord> loadData(File file){
        List<DataRecord> dataSet = new LinkedList<>();

        try(InputStream input = new FileInputStream(file)){
            Workbook workbook = new HSSFWorkbook(input);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.iterator();

            List<String> headers = new ArrayList<>();

            if(iterator.hasNext()){
                Row headerRow = iterator.next();
                if (iterator.hasNext())
                    iterator.next();
                for(Cell cell : headerRow){
                    headers.add(cell.getStringCellValue());
                    log.info("header {} loaded",cell.getStringCellValue());
                }
                while(iterator.hasNext()){
                    Long id =0L;
                    Row row = iterator.next();
                    var dataObj = new DataRecord();

                    for (int i=0; i<headers.size();i++){
                        if (i<headers.size()-1) {
                            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            Long value = (long) cell.getNumericCellValue();

                            dataObj.setId(id++);
                            dataObj.getAttributes().put(headers.get(i), value);
                        }else {
                            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            dataObj.setY((long) cell.getNumericCellValue());
                        }
                    }
                    dataSet.add(dataObj);
                    log.info("data object {} loaded",dataObj);
                }
                return dataSet;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dataSet;
    }
}
