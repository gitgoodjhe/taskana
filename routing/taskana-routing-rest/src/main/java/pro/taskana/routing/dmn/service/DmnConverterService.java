package pro.taskana.routing.dmn.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.camunda.bpm.dmn.xlsx.AdvancedSpreadsheetAdapter;
import org.camunda.bpm.dmn.xlsx.XlsxConverter;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.api.TaskanaRole;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.common.api.exceptions.SystemException;
import pro.taskana.common.internal.util.Pair;
import pro.taskana.routing.dmn.service.util.InputEntriesSanitizer;
import pro.taskana.workbasket.api.WorkbasketService;

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
      validateOutputs(dmnModelInstance);

      InputEntriesSanitizer.sanitizeFunctionsInsideInputEntries(dmnModelInstance);

      return dmnModelInstance;
    }
  }

  private Set<Pair<String, String>> getAllWorkbasketAndDomainOutputs(DmnModelInstance dmnModel) {
    Set<Pair<String, String>> allWorkbasketAndDomainOutputs = new HashSet<>();

    for (Rule rule : dmnModel.getModelElementsByType(Rule.class)) {

      List<OutputEntry> outputEntries = new ArrayList<>(rule.getOutputEntries());
      String workbasketKey = outputEntries.get(0).getTextContent();
      String domain = outputEntries.get(1).getTextContent();

      allWorkbasketAndDomainOutputs.add(Pair.of(workbasketKey, domain));
    }
    return allWorkbasketAndDomainOutputs;
  }

  private void validateOutputs(DmnModelInstance dmnModel) {
    Set<Pair<String, String>> allWorkbasketAndDomainOutputs =
        getAllWorkbasketAndDomainOutputs(dmnModel);

    validate(allWorkbasketAndDomainOutputs);
  }

  private void validate(Set<Pair<String, String>> allWorkbasketAndDomainOutputs) {
    WorkbasketService workbasketService = taskanaEngine.getWorkbasketService();

    for (Pair<String, String> pair : allWorkbasketAndDomainOutputs) {
      String workbasketKey = pair.getLeft().replace("\"", "");
      String domain = pair.getRight().replace("\"", "");

      taskanaEngine.runAsAdmin(
          () -> {
            try {
              return workbasketService.getWorkbasket(workbasketKey, domain);
            } catch (Exception e) {
              throw new SystemException(
                  String.format(
                      "Unknown workbasket defined in DMN Table. key: '%s', domain: '%s'",
                      workbasketKey, domain),
                  e);
            }
          });
    }
  }
}
