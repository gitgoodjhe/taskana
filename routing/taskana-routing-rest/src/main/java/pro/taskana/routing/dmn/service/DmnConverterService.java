package pro.taskana.routing.dmn.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.camunda.bpm.dmn.xlsx.AdvancedSpreadsheetAdapter;
import org.camunda.bpm.dmn.xlsx.XlsxConverter;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import pro.taskana.routing.dmn.service.util.InputEntriesSanitizer;

@Service
public class DmnConverterService {

  public DmnModelInstance convertExcelToDmn(MultipartFile excelRoutingFile) throws IOException {

    try (InputStream inputStream = new BufferedInputStream(excelRoutingFile.getInputStream())) {

      XlsxConverter converter = new XlsxConverter();
      converter.setIoDetectionStrategy(new AdvancedSpreadsheetAdapter());

      DmnModelInstance dmnModelInstance = converter.convert(inputStream);

      InputEntriesSanitizer.sanitizeFunctionsInsideInputEntries(dmnModelInstance);

      return dmnModelInstance;
    }
  }
}
