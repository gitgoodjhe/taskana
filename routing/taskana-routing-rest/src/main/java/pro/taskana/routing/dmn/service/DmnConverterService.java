package pro.taskana.routing.dmn.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.camunda.bpm.dmn.xlsx.AdvancedSpreadsheetAdapter;
import org.camunda.bpm.dmn.xlsx.XlsxConverter;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import pro.taskana.common.api.KeyDomain;
import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.api.TaskanaRole;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.common.api.exceptions.SystemException;
import pro.taskana.routing.dmn.service.util.InputEntriesSanitizer;
import pro.taskana.workbasket.api.WorkbasketService;

/** This class converts an Excel file with routing roules to a DMN table. */
@Service
public class DmnConverterService {

  private final TaskanaEngine taskanaEngine;

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

  private Set<KeyDomain> getOutputKeyDomains(DmnModelInstance dmnModel) {
    Set<KeyDomain> outputKeyDomains = new HashSet<>();

    for (Rule rule : dmnModel.getModelElementsByType(Rule.class)) {

      List<OutputEntry> outputEntries = new ArrayList<>(rule.getOutputEntries());
      String workbasketKey = outputEntries.get(0).getTextContent().replaceAll("(^\")|(\"$)", "");
      String domain = outputEntries.get(1).getTextContent().replaceAll("(^\")|(\"$)", "");
      outputKeyDomains.add(new KeyDomain(workbasketKey, domain));
    }
    return outputKeyDomains;
  }

  private void validateOutputs(DmnModelInstance dmnModel) {
    Set<KeyDomain> allWorkbasketAndDomainOutputs = getOutputKeyDomains(dmnModel);
    validate(allWorkbasketAndDomainOutputs);
  }

  private void validate(Set<KeyDomain> outputKeyDomains) {

    Set<KeyDomain> existingKeyDomains = getExistingKeyDomains();

    outputKeyDomains.removeAll(existingKeyDomains);

    if (!outputKeyDomains.isEmpty()) {
      throw new SystemException(
          String.format(
              "Unknown workbasket Key/Domain pairs defined in DMN Table: %s", outputKeyDomains));
    }
  }

  private Set<KeyDomain> getExistingKeyDomains() {

    WorkbasketService workbasketService = taskanaEngine.getWorkbasketService();

    return taskanaEngine.runAsAdmin(
        () ->
            workbasketService.createWorkbasketQuery().list().stream()
                .map(
                    workbasketSummary ->
                        new KeyDomain(workbasketSummary.getKey(), workbasketSummary.getDomain()))
                .collect(Collectors.toSet()));
  }
}
