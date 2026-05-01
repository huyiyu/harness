package com.harness.lifecycle.oauth;

import com.harness.auth.gitlab.GitLabApi;
import com.harness.auth.gitlab.dto.GitLabOAuthUserInfo;
import com.harness.auth.gitlab.dto.GitLabUserResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.mockito.Mockito.when;

class GitLabUserInfoControllerTest {

    @Test
    void userInfo_returnsGitLabUserInfo() {
        GitLabApi gitLabApi = Mockito.mock(GitLabApi.class);
        when(gitLabApi.findUsers("wechat_mock_openid_001"))
                .thenReturn(List.of(new GitLabUserResponse(3L, "wechat_mock_openid_001", "mock_openid_001@wechat.mock")));

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("mock_openid_001");

        GitLabUserInfoController controller = new GitLabUserInfoController(gitLabApi);
        GitLabOAuthUserInfo result = controller.userInfo(jwt);

        assert result.id() == 3L;
        assert result.username().equals("wechat_mock_openid_001");
    }
}
