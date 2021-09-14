package pro.taskana.task.internal;

import java.util.List;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.common.api.TaskanaRole;
import pro.taskana.common.api.TimeInterval;
import pro.taskana.common.internal.InternalTaskanaEngine;
import pro.taskana.task.api.TaskCommentQuery;
import pro.taskana.task.api.TaskCommentQueryColumnName;
import pro.taskana.task.api.models.TaskComment;
import pro.taskana.workbasket.internal.WorkbasketQueryImpl;

/** TaskCommentQuery for generating dynamic sql. */
public class TaskCommentQueryImpl implements TaskCommentQuery {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskCommentQueryImpl.class);

  private static final String LINK_TO_MAPPER =
      "pro.taskana.task.internal.TaskCommentQueryMapper.queryTaskComments";

  private static final String LINK_TO_VALUE_MAPPER =
      "pro.taskana.task.internal.TaskCommentQueryMapper.queryTaskCommentColumnValues";

  private static final String LINK_TO_COUNTER =
      "pro.taskana.task.internal.TaskCommentQueryMapper.countQueryTaskComments";

  private final InternalTaskanaEngine taskanaEngine;
  private final TaskServiceImpl taskService;
  private TaskCommentQueryColumnName queryColumnName;
  private String[] idIn;
  private String[] taskIdIn;
  private String[] accessIdIn;
  private String[] taskIdLike;
  private String[] creatorIn;
  private String[] creatorLike;
  private String[] textFieldLike;
  private TimeInterval[] modifiedIn;
  private TimeInterval[] createdIn;
  private boolean joinWithUserInfo;

  TaskCommentQueryImpl(InternalTaskanaEngine taskanaEngine) {
    this.taskanaEngine = taskanaEngine;
    this.taskService = (TaskServiceImpl) taskanaEngine.getEngine().getTaskService();
    // read property here
    String readUserInfoProperty =
        taskanaEngine
            .getEngine()
            .getConfiguration()
            .readPropertiesFromFile()
            .getProperty("taskana.read.user.info");
    if (readUserInfoProperty != null) {
      this.joinWithUserInfo = Boolean.parseBoolean(readUserInfoProperty);
    }
  }

  @Override
  public TaskCommentQuery idIn(String... taskCommentIds) {
    this.idIn = taskCommentIds;
    return this;
  }

  @Override
  public TaskCommentQuery taskIdIn(String... taskIds) {
    this.taskIdIn = taskIds;
    return this;
  }

  @Override
  public TaskCommentQuery taskIdLike(String... taskIds) {
    this.taskIdLike = taskIds;
    return this;
  }

  @Override
  public TaskCommentQuery textFieldLike(String... texts) {
    this.textFieldLike = texts;
    return this;
  }

  @Override
  public TaskCommentQuery creatorIn(String... creators) {
    this.creatorIn = creators;
    return this;
  }

  @Override
  public TaskCommentQuery creatorLike(String... creators) {
    this.creatorLike = creators;
    return this;
  }

  @Override
  public TaskCommentQuery createdWithin(TimeInterval... intervals) {
    validateAllIntervals(intervals);
    this.createdIn = intervals;
    return this;
  }

  @Override
  public TaskCommentQuery modifiedWithin(TimeInterval... intervals) {
    validateAllIntervals(intervals);
    this.modifiedIn = intervals;
    return this;
  }

  @Override
  public List<TaskComment> list() {
    setupAccessIds();
    return taskanaEngine.executeInDatabaseConnection(
        () -> taskanaEngine.getSqlSession().selectList(LINK_TO_MAPPER, this));
  }

  @Override
  public List<TaskComment> list(int offset, int limit) {
    setupAccessIds();
    RowBounds rowBounds = new RowBounds(offset, limit);
    return taskanaEngine.executeInDatabaseConnection(
        () -> taskanaEngine.getSqlSession().selectList(LINK_TO_MAPPER, this, rowBounds));
  }

  @Override
  public List<String> listValues(
      TaskCommentQueryColumnName columnName, SortDirection sortDirection) {
    setupAccessIds();
    queryColumnName = columnName;
    // TO-DO: order?
    return taskanaEngine.executeInDatabaseConnection(
        () -> taskanaEngine.getSqlSession().selectList(LINK_TO_VALUE_MAPPER, this));
  }

  @Override
  public TaskComment single() {
    setupAccessIds();
    return taskanaEngine.executeInDatabaseConnection(
        () -> taskanaEngine.getSqlSession().selectOne(LINK_TO_MAPPER, this));
  }

  @Override
  public long count() {
    setupAccessIds();
    Long rowCount =
        taskanaEngine.executeInDatabaseConnection(
            () -> taskanaEngine.getSqlSession().selectOne(LINK_TO_COUNTER, this));
    return (rowCount == null) ? 0L : rowCount;
  }

  private void setupAccessIds() {
    if (taskanaEngine.getEngine().isUserInRole(TaskanaRole.ADMIN, TaskanaRole.TASK_ADMIN)) {
      this.accessIdIn = null;
    } else if (this.accessIdIn == null) {
      String[] accessIds = new String[0];
      List<String> ucAccessIds = taskanaEngine.getEngine().getCurrentUserContext().getAccessIds();
      if (!ucAccessIds.isEmpty()) {
        accessIds = new String[ucAccessIds.size()];
        accessIds = ucAccessIds.toArray(accessIds);
      }
      this.accessIdIn = accessIds;
      WorkbasketQueryImpl.lowercaseAccessIds(this.accessIdIn);
    }
  }

  private void validateAllIntervals(TimeInterval[] intervals) {
    for (TimeInterval ti : intervals) {
      if (!ti.isValid()) {
        throw new IllegalArgumentException("TimeInterval " + ti + " is invalid.");
      }
    }
  }
}
