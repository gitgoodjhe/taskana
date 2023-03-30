package acceptance.task.query;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.taskana.testapi.DefaultTestEntities.defaultTestClassification;
import static pro.taskana.testapi.DefaultTestEntities.defaultTestObjectReference;
import static pro.taskana.testapi.DefaultTestEntities.defaultTestWorkbasket;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import pro.taskana.TaskanaConfiguration;
import pro.taskana.classification.api.ClassificationService;
import pro.taskana.classification.api.models.ClassificationSummary;
import pro.taskana.common.api.BaseQuery.SortDirection;
import pro.taskana.common.api.security.CurrentUserContext;
import pro.taskana.task.api.TaskService;
import pro.taskana.task.api.models.ObjectReference;
import pro.taskana.task.api.models.TaskSummary;
import pro.taskana.testapi.TaskanaConfigurationModifier;
import pro.taskana.testapi.TaskanaInject;
import pro.taskana.testapi.TaskanaIntegrationTest;
import pro.taskana.testapi.builder.ObjectReferenceBuilder;
import pro.taskana.testapi.builder.TaskBuilder;
import pro.taskana.testapi.builder.WorkbasketAccessItemBuilder;
import pro.taskana.testapi.security.WithAccessId;
import pro.taskana.workbasket.api.WorkbasketPermission;
import pro.taskana.workbasket.api.WorkbasketService;
import pro.taskana.workbasket.api.models.WorkbasketSummary;

@TaskanaIntegrationTest
public class TaskQueryImplGroupByAccTest {
  @Nested
  @TestInstance(Lifecycle.PER_CLASS)
  class WithCommonQueryEnforceSetToTrue implements TaskanaConfigurationModifier {
    @TaskanaInject TaskService taskService;
    @TaskanaInject WorkbasketService workbasketService;
    @TaskanaInject CurrentUserContext currentUserContext;
    @TaskanaInject ClassificationService classificationService;

    ClassificationSummary defaultClassificationSummary;
    WorkbasketSummary defaultWorkbasket;
    TaskSummary taskSummary1;
    TaskSummary taskSummary2;
    TaskSummary taskSummary3;

    @Override
    public TaskanaConfiguration.Builder modify(TaskanaConfiguration.Builder builder) {
      return builder.commonQueryUsageEnforce(true);
    }

    @WithAccessId(user = "user-1-1")
    @BeforeAll
    void setup() throws Exception {
      defaultClassificationSummary =
          defaultTestClassification()
              .buildAndStoreAsSummary(classificationService, "businessadmin");
      defaultWorkbasket = createWorkbasketWithPermission();
      ObjectReference por1 = defaultTestObjectReference().company("15").build();
      ObjectReference sor1 =
          ObjectReferenceBuilder.newObjectReference()
              .company("FirstCompany")
              .value("FirstValue")
              .type("FirstType")
              .build();
      taskSummary1 =
          taskInWorkbasket(defaultWorkbasket)
              .primaryObjRef(por1)
              .objectReferences(sor1)
              .due(Instant.parse("2022-11-09T09:42:00.000Z"))
              .name("Name3")
              .buildAndStoreAsSummary(taskService);
      ObjectReference por2 = defaultTestObjectReference().build();
      ObjectReference sor2 =
          ObjectReferenceBuilder.newObjectReference()
              .company("FirstCompany")
              .value("FirstValue")
              .type("SecondType")
              .build();
      taskSummary2 =
          taskInWorkbasket(defaultWorkbasket)
              .primaryObjRef(por2)
              .objectReferences(sor2)
              .due(Instant.parse("2022-11-10T09:45:00.000Z"))
              .name("Name2")
              .buildAndStoreAsSummary(taskService);
      ObjectReference sor2copy = sor2.copy();
      ObjectReference sor1copy = sor1.copy();
      taskSummary3 =
          taskInWorkbasket(defaultWorkbasket)
              .objectReferences(sor2copy, sor1copy)
              .due(Instant.parse("2022-11-15T09:45:00.000Z"))
              .name("Name1")
              .buildAndStoreAsSummary(taskService);
      taskInWorkbasket(createWorkbasketWithPermission()).buildAndStore(taskService);
    }

    private TaskBuilder taskInWorkbasket(WorkbasketSummary wb) {
      return TaskBuilder.newTask()
          .classificationSummary(defaultClassificationSummary)
          .primaryObjRef(defaultTestObjectReference().build())
          .workbasketSummary(wb);
    }

    private WorkbasketSummary createWorkbasketWithPermission() throws Exception {
      WorkbasketSummary workbasketSummary =
          defaultTestWorkbasket().buildAndStoreAsSummary(workbasketService, "businessadmin");
      persistPermission(workbasketSummary);
      return workbasketSummary;
    }

    private void persistPermission(WorkbasketSummary workbasketSummary) throws Exception {
      WorkbasketAccessItemBuilder.newWorkbasketAccessItem()
          .workbasketId(workbasketSummary.getId())
          .accessId(currentUserContext.getUserid())
          .permission(WorkbasketPermission.OPEN)
          .permission(WorkbasketPermission.READ)
          .permission(WorkbasketPermission.APPEND)
          .buildAndStore(workbasketService, "businessadmin");
    }

    @WithAccessId(user = "user-1-1")
    @Test
    void should_GroupByPor_When_OrderingByName() {
      List<TaskSummary> list =
          taskService
              .createTaskQuery()
              .workbasketIdIn(defaultWorkbasket.getId())
              .groupByPor()
              .orderByName(SortDirection.ASCENDING)
              .list();
      assertThat(list).isNotEmpty().hasSize(1);
      assertThat(list.get(0))
          .usingRecursiveComparison()
          .ignoringFields("groupByCount")
          .isEqualTo(taskSummary3);
      assertThat(list.get(0).getGroupByCount()).isEqualTo(3);
    }

    @WithAccessId(user = "user-1-1")
    @Test
    void should_GroupByPorWithOrderingByDue_When_OrderingByPorValue() {
      List<TaskSummary> list =
          taskService
              .createTaskQuery()
              .workbasketIdIn(defaultWorkbasket.getId())
              .groupByPor()
              .orderByPrimaryObjectReferenceValue(SortDirection.ASCENDING)
              .list();
      assertThat(list).isNotEmpty().hasSize(1);
      assertThat(list.get(0))
          .usingRecursiveComparison()
          .ignoringFields("groupByCount")
          .isEqualTo(taskSummary1);
      assertThat(list.get(0).getGroupByCount()).isEqualTo(3);
    }

    @WithAccessId(user = "user-1-1")
    @Test
    void should_GroupByPorWithOrderingByDue_When_NotOrdering() {
      List<TaskSummary> list =
          taskService
              .createTaskQuery()
              .workbasketIdIn(defaultWorkbasket.getId())
              .groupByPor()
              .list();
      assertThat(list).isNotEmpty().hasSize(1);
      assertThat(list.get(0))
          .usingRecursiveComparison()
          .ignoringFields("groupByCount")
          .isEqualTo(taskSummary1);
      assertThat(list.get(0).getGroupByCount()).isEqualTo(3);
    }

    @WithAccessId(user = "user-1-1")
    @Test
    void should_Count_When_GroupingByPor() {
      Long numberOfTasks =
          taskService
              .createTaskQuery()
              .workbasketIdIn(defaultWorkbasket.getId())
              .groupByPor()
              .count();
      assertThat(numberOfTasks).isEqualTo(1);
    }

    @WithAccessId(user = "user-1-1")
    @Test
    void should_GroupBySor_When_OrderingByName() {
      List<TaskSummary> list =
          taskService
              .createTaskQuery()
              .workbasketIdIn(defaultWorkbasket.getId())
              .groupBySor("SecondType")
              .orderByName(SortDirection.ASCENDING)
              .list();
      assertThat(list).isNotEmpty().hasSize(1);
      assertThat(list.get(0))
          .usingRecursiveComparison()
          .ignoringFields("groupByCount")
          .isEqualTo(taskSummary3);
      assertThat(list.get(0).getGroupByCount()).isEqualTo(2);
    }

    @WithAccessId(user = "user-1-1")
    @Test
    void should_GroupBySorWithOrderingByDue_When_NotOrdering() {
      List<TaskSummary> list =
          taskService
              .createTaskQuery()
              .workbasketIdIn(defaultWorkbasket.getId())
              .groupBySor("SecondType")
              .list();
      assertThat(list).hasSize(1);
      assertThat(list.get(0))
          .usingRecursiveComparison()
          .ignoringFields("groupByCount")
          .isEqualTo(taskSummary2);
      assertThat(list.get(0).getGroupByCount()).isEqualTo(2);
    }

    @WithAccessId(user = "user-1-1")
    @Test
    void should_Count_When_GroupingBySor() {
      Long numberOfTasks =
          taskService
              .createTaskQuery()
              .workbasketIdIn(defaultWorkbasket.getId())
              .groupBySor("SecondType")
              .count();
      assertThat(numberOfTasks).isEqualTo(1);
    }
  }
}
