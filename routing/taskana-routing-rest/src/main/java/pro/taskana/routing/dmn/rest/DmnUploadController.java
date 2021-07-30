package pro.taskana.routing.dmn.rest;

import java.io.File;
import java.io.IOException;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.routing.dmn.service.DmnConverterService;
import pro.taskana.routing.dmn.spi.internal.DmnValidatorManager;

/** Controller for all DMN upload related endpoints. */
@RestController
public class DmnUploadController {

  private final DmnConverterService dmnConverterService;
  private final TaskanaEngine taskanaEngine;

  @Value("${dmn.upload.path}")
  String dmnUploadPath;

  @Autowired
  public DmnUploadController(DmnConverterService dmnConverterService, TaskanaEngine taskanaEngine) {
    this.dmnConverterService = dmnConverterService;
    this.taskanaEngine = taskanaEngine;
    DmnValidatorManager.getInstance(taskanaEngine);
  }

  /**
   * This endpoint converts an excel file to a DMN table and saves it on the filesystem.
   *
   * @param excelRoutingFile the excel file containing the routing rules
   * @return the result of the upload
   * @throws IOException if there is an I/O problem with the provided excel file
   */
  @PostMapping(RoutingRestEndpoints.URL_DMN)
  public ResponseEntity<RoutingUploadResultRepresentationModel> convertAndUpload(
      @RequestParam("excelRoutingFile") MultipartFile excelRoutingFile) throws IOException {

    DmnModelInstance dmnModelInstance = dmnConverterService.convertExcelToDmn(excelRoutingFile);

    if (DmnValidatorManager.isDmnUploadProviderEnabled()) {
      DmnValidatorManager.getInstance(taskanaEngine).validate(dmnModelInstance);
    }

    File uploadDestinationFile = new File(dmnUploadPath);
    Dmn.writeModelToFile(uploadDestinationFile, dmnModelInstance);

    int importedRows = dmnModelInstance.getModelElementsByType(Rule.class).size();

    RoutingUploadResultRepresentationModel model = new RoutingUploadResultRepresentationModel();
    model.setAmountOfImportedRows(importedRows);
    model.setResult(
        "Successfully imported " + importedRows + " routing rules from the provided excel sheet");

    return ResponseEntity.ok(model);
  }

  /**
   * This endpoint checks if the taskana-routing-rest is in use.
   *
   * @return true, when the taskana-routing-rest is enabled, otherwise false
   */
  @GetMapping(path = RoutingRestEndpoints.ROUTING_REST_ENABLED)
  public ResponseEntity<Boolean> getIsRoutingRestEnabled() {
    return ResponseEntity.ok(DmnValidatorManager.isDmnUploadProviderEnabled());
  }
}
