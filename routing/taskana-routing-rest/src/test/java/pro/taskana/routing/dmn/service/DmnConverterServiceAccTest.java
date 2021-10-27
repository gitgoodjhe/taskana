package pro.taskana.routing.dmn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.api.TaskanaRole;

@ExtendWith(MockitoExtension.class)
class DmnConverterServiceAccTest {

  private static final String EXCEL_NAME = "testExcelRouting.xlsx";
  @Mock TaskanaEngine taskanaEngine;

  @Test
  void should_ConvertExcelToDmn() throws Exception {

    doNothing()
        .when(taskanaEngine)
        .checkRoleMembership(TaskanaRole.ADMIN, TaskanaRole.BUSINESS_ADMIN);

    File excelRoutingFile = new ClassPathResource(EXCEL_NAME).getFile();
    InputStream targetStream = new FileInputStream(excelRoutingFile);

    MultipartFile routingMultiPartFile = new MockMultipartFile(EXCEL_NAME, targetStream);

    DmnConverterService dmnConverterService = new DmnConverterService(taskanaEngine);
    DmnModelInstance dmnModelInstance = dmnConverterService.convertExcelToDmn(routingMultiPartFile);

    assertThat(dmnModelInstance.getModelElementsByType(Rule.class)).hasSize(3);
  }
}
