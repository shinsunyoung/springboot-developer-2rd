## 추가내용. OAuth 로그아웃 기능 추가
이 프로젝트에서는 토큰을 수동으로 삭제하여 로그아웃을 마무리했습니다. 하지만 이 기능을 코드로 구현하려면 어떻게 해야할까요?

1. 로그아웃 버튼을 누르면 2. 웹에서는 저장하고 있는 액세스 토큰, 리프레시 토큰을 삭제하고 3. 서버에서는 저장되어 있는 리프레시 토큰을 삭제함으로서 로그아웃 처리를 하면 됩니다.
이 순서대로 하나씩 구현해보도록 하겠습니다.
여기에서 구현하는 전체 코드 변경은 https://github.com/shinsunyoung/springboot-developer-2rd/commit/8c54b2a50a1df89276eaad69848a896cbcad5083 을 참고해주세요.

### 1. 로그아웃 버튼 

기존에는 로그아웃 버튼은 클릭하면 /logout GET HTTP 요청을 보내는 동작을 하게 되어있습니다. (`onclick="location.href='/logout'`) OAuth로 바꾼 이후에 해당 API는 유효하지 않기 때문에 호출하는 코드를 제거합니다. 위에서 설명한 것 처럼 웹에서는 저장하고 있는 액세스 토큰, 리프레시 토큰을 삭제해야 하기 때문에 id를 추가합니다.

- src/main/resources/templates/articleList.html
```html
<button type="button" class="btn btn-secondary" id="logout-btn">로그아웃</button>
```

### 2. 웹에서 토큰 삭제 
그 이후에는 자바스크립트 파일을 추가하여 웹에 있는 액세스 토큰, 리프레시 토큰을 삭제하는 로직과 리프레시 토큰을 삭제하는 API를 요청하는 로직을 추가합니다.

- src/main/resources/static/js/article.js
```javascript
// 로그아웃 기능
const logoutButton = document.getElementById('logout-btn');

if (logoutButton) {
    logoutButton.addEventListener('click', event => {
        function success() {
            // 로컬 스토리지에 저장된 액세스 토큰을 삭제
            localStorage.removeItem('access_token');

            // 쿠키에 저장된 리프레시 토큰을 삭제
            deleteCookie('refresh_token');
            location.replace('/login');
        }
        function fail() {
            alert('로그아웃 실패했습니다.');
        }

        httpRequest('DELETE','/api/refresh-token', null, success, fail);
    });
}


// 쿠키를 삭제하는 함수
function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}
```


### 3. 리프레시 토큰 삭제 API
리프레시 토큰을 삭제하는 API를 추가합니다. 컨트롤러에 Http Method가 DELETE이고 Path가 /api/refresh-token 일 때, 요청에 걸리는 로직을 컨트롤러에 추가합니다.

- src/main/java/me/shinsunyoung/springbootdeveloper/controller/TokenApiController.java
```java
@RestController
public class TokenApiController {
    ...
    private final RefreshTokenService refreshTokenService;

    @DeleteMapping("/api/refresh-token")
    public ResponseEntity deleteRefreshToken() {
        refreshTokenService.delete();

        return ResponseEntity.ok()
                .build();
    }
}
```

그 뒤에는 현재 인증정보를 가져온 뒤, 유저 아이디에 해당하는 리프레시 토큰을 삭제하는 로직을 작성합니다.

- src/main/java/me/shinsunyoung/springbootdeveloper/service/RefreshTokenService.java
```java
@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    ...
    private final TokenProvider tokenProvider;

    ...

    @Transactional
    public void delete() {
        String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        Long userId = tokenProvider.getUserId(token);

        refreshTokenRepository.deleteByUserId(userId);
    }
}
```

- src/main/java/me/shinsunyoung/springbootdeveloper/repository/RefreshTokenRepository.java

```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    ...
    void deleteByUserId(Long userId);
}
```

이러면 OAuth 로그인 방식을 사용할 때 로그아웃 기능을 구현할 수 있습니다. 
로그아웃 방법은 리프레시 토큰을 데이터베이스에서 삭제하는 방법 말고도 블랙리스트 리프레시 토큰을 관리한다거나, 유저와 리프레시 토큰이 1:N인 경우에는 특정 리프레시 토큰만 삭제하기도 합니다. 위에서 구현한 방법은 가장 간단하게 로그아웃을 할 수 있는 방법을 구현한 것이니 구현하려는 서비스의 특성에 맞게 로그아웃을 구현해야 합니다.

### 4. 테스트 코드 작성
유저의 리프레시 토큰이 잘 삭제되는지 테스트하는 코드를 작성합니다. 테스트를 실행하기 전에는 유저의 정보를 SecurityContextHolder에 저장하고, 리프레시 토큰 삭제 요청을 보냈을 때 저장된 유저의 리프레시 토큰이 잘 삭제되는지 검증합니다.

- src/test/java/me/shinsunyoung/springbootdeveloper/controller/TokenApiControllerTest.java

```java
class TokenApiControllerTest {
  User user;

    @BeforeEach
    void setSecurityContext() {
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
    }

    @DisplayName("deleteRefreshToken: 리프레시 토큰을 삭제한다.")
    @Test
    public void deleteRefreshToken() throws Exception {
        // given
        final String url = "/api/refresh-token";

        String refreshToken = createRefreshToken();

        refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken));

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, refreshToken, user.getAuthorities()));

        // when
        ResultActions resultActions = mockMvc.perform(delete(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE));

        // then
        resultActions
                .andExpect(status().isOk());

        assertThat(refreshTokenRepository.findByRefreshToken(refreshToken)).isEmpty();
    }


    private String createRefreshToken() {
        return JwtFactory.builder()
                .claims(Map.of("id", user.getId()))
                .build()
                .createToken(jwtProperties);
    }

}
```
