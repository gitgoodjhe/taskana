package pro.taskana.routing.dmn.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.camunda.bpm.dmn.xlsx.AdvancedSpreadsheetAdapter;
import org.camunda.bpm.dmn.xlsx.XlsxConverter;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.api.TaskanaRole;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.routing.dmn.service.util.InputEntriesSanitizer;

/** This class converts an Excel file with routing roules to a DMN table. */
@Service
public class DmnConverterService {

  private TaskanaEngine taskanaEngine;

  @Autowired
  public DmnConverterService(TaskanaEngine taskanaEngine) {
    this.taskanaEngine = taskanaEngine;
  }

  public DmnModelInstance convertExcelToDmn(MultipartFile excelRoutingFile)
      throws IOException, NotAuthorizedException {

    taskanaEngine.checkRoleMembership(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN);

    try (InputStream inputStream = new BufferedInputStream(excelRoutingFile.getInputStream())) {

      XlsxConverter converter = new XlsxConverter();
      converter.setIoDetectionStrategy(new AdvancedSpreadsheetAdapter());

      DmnModelInstance dmnModelInstance = converter.convert(inputStream);

      InputEntriesSanitizer.sanitizeFunctionsInsideInputEntries(dmnModelInstance);

      return dmnModelInstance;
    }
  }
}
