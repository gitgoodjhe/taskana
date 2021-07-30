package pro.taskana.routing.dmn.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class DmnConverterServiceAccTest {

  private static final String EXCEL_NAME = "testExcelRouting.xlsx";

  @Test
  void should_ConvertExcelToDmn() throws Exception {

    File excelRoutingFile = new ClassPathResource(EXCEL_NAME).getFile();
    InputStream targetStream = new FileInputStream(excelRoutingFile);

    MultipartFile routingMultiPartFile = new MockMultipartFile(EXCEL_NAME, targetStream);

    DmnConverterService dmnConverterService = new DmnConverterService();
    DmnModelInstance dmnModelInstance = dmnConverterService.convertExcelToDmn(routingMultiPartFile);

    assertThat(dmnModelInstance.getModelElementsByType(Rule.class)).hasSize(3);
  }
}
