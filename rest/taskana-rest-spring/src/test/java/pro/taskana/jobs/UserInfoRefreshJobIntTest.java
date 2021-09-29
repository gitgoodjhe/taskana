package pro.taskana.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.rest.ldap.LdapClient;
import pro.taskana.common.test.rest.TaskanaSpringBootTest;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.user.api.UserService;
import pro.taskana.user.api.models.User;
import pro.taskana.user.internal.models.UserImpl;

@TaskanaSpringBootTest
@ExtendWith(JaasExtension.class)
public class UserInfoRefreshJobIntTest {

  TaskanaEngine taskanaEngine;
  UserService userService;
  LdapClient ldapClient;

  @Autowired
  public UserInfoRefreshJobIntTest(TaskanaEngine taskanaEngine, UserService userService,
      LdapClient ldapClient) {
    this.taskanaEngine = taskanaEngine;
    this.userService = userService;
    this.ldapClient = ldapClient;
  }

  @Test
  @WithAccessId(user = "businessadmin")
  void should_RefreshUserInfo_When_UserInfoRefreshJobIsExecuted() throws Exception {

    try (Connection connection = taskanaEngine.getConfiguration().getDatasource().getConnection()) {

      List<User> users = getUsers(connection);

      assertThat(users).hasSize(14);

      UserInfoRefreshJob userInfoRefreshJob = new UserInfoRefreshJob(taskanaEngine);
      userInfoRefreshJob.execute();

      users = getUsers(connection);

      assertThat(users).hasSize(6);

      List<User> ldapusers = ldapClient.searchUsersInUserRole();

      assertThat(users).hasSameElementsAs(ldapusers);

      List<String> ids = users.stream().map(user -> user.getId()).collect(Collectors.toList());

      assertThat(ids)
          .containsExactly(
              "teamlead-1", "teamlead-2", "user-1-1", "user-1-2", "user-2-1", "user-2-2");
    }
  }

  private List<User> getUsers(Connection connection) throws Exception {

    List<User> users = new ArrayList<>();
    Statement statement = connection.createStatement();
    ResultSet rs = statement.executeQuery("SELECT * FROM " + connection.getSchema() + ".USER_INFO");

    while (rs.next()) {
      User ldapUser = new UserImpl();
      ldapUser.setId(rs.getString("USER_ID"));
      ldapUser.setFirstName(rs.getString("FIRST_NAME"));
      ldapUser.setLastName(rs.getString("LASTNAME"));
      ldapUser.setFullName(rs.getString("FULL_NAME"));
      ldapUser.setLongName(rs.getString("LONG_NAME"));
      ldapUser.setEmail(rs.getString("E_MAIL"));
      ldapUser.setPhone(rs.getString("PHONE"));
      ldapUser.setMobilePhone(rs.getString("MOBILE_PHONE"));
      ldapUser.setOrgLevel4(rs.getString("ORG_LEVEL_4"));
      ldapUser.setOrgLevel3(rs.getString("ORG_LEVEL_3"));
      ldapUser.setOrgLevel2(rs.getString("ORG_LEVEL_2"));
      ldapUser.setOrgLevel1(rs.getString("ORG_LEVEL_1"));
      ldapUser.setData(rs.getString("DATA"));

      users.add(ldapUser);
    }

    return users;
  }
}
