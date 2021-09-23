package pro.taskana.jobs;

import java.sql.PreparedStatement;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.common.api.ScheduledJob;
import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.api.exceptions.InvalidArgumentException;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.common.api.exceptions.SystemException;
import pro.taskana.common.internal.JobServiceImpl;
import pro.taskana.common.internal.jobs.AbstractTaskanaJob;
import pro.taskana.common.internal.transaction.TaskanaTransactionProvider;
import pro.taskana.common.rest.ldap.LdapClient;
import pro.taskana.task.internal.jobs.helper.SqlConnectionRunner;
import pro.taskana.user.api.exceptions.UserAlreadyExistException;
import pro.taskana.user.api.models.User;

public class UserRefreshJob extends AbstractTaskanaJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserRefreshJob.class);
  private final SqlConnectionRunner sqlConnectionRunner;

  public UserRefreshJob(TaskanaEngine taskanaEngine) {
    this(taskanaEngine, null, null);
  }

  public UserRefreshJob(
      TaskanaEngine taskanaEngine,
      TaskanaTransactionProvider txProvider,
      ScheduledJob scheduledJob) {
    super(taskanaEngine, txProvider, scheduledJob, true);
    runEvery = taskanaEngine.getConfiguration().getUserRefreshJobRunEvery();
    firstRun = taskanaEngine.getConfiguration().getUserRefreshJobFirstRun();
    sqlConnectionRunner = new SqlConnectionRunner(taskanaEngine);
  }

  /**
   * Initializes the {@linkplain UserRefreshJob} schedule. <br>
   * All scheduled jobs are cancelled/deleted and a new one is scheduled.
   *
   * @param taskanaEngine the TASKANA engine.
   */
  public static void initializeSchedule(TaskanaEngine taskanaEngine) {
    JobServiceImpl jobService = (JobServiceImpl) taskanaEngine.getJobService();
    UserRefreshJob job = new UserRefreshJob(taskanaEngine);
    jobService.deleteJobs(job.getType());
    job.scheduleNextJob();
  }

  @Override
  protected String getType() {
    return UserRefreshJob.class.getName();
  }

  @Override
  protected void execute() {
    LOGGER.info("Running job to refresh all user info");

    try {

      LdapClient ldapClient =
          ApplicationContextProvider.getApplicationContext()
              .getBean("ldapClient", LdapClient.class);
      List<User> users = ldapClient.searchUsersInUserRole();

      if (!users.isEmpty()) {
        sqlConnectionRunner.runWithConnection(
            connection -> {
              String schema = taskanaEngineImpl.getConfiguration().getSchemaName();
              String sql = "DELETE FROM " + schema + ".USER_INFO";
              PreparedStatement statement = connection.prepareStatement(sql);
              statement.execute();
            });

        users.forEach(
            user -> {
              try {
                taskanaEngineImpl.getUserService().createUser(user);
              } catch (InvalidArgumentException e) {
                e.printStackTrace();
              } catch (NotAuthorizedException e) {
                e.printStackTrace();
              } catch (UserAlreadyExistException e) {
                e.printStackTrace();
              }
            });
        LOGGER.info("Job to refresh all user info has finished.");
      }
    } catch (Exception e) {
      throw new SystemException("Error while processing UserRefreshJob.", e);
    }
  }
}
