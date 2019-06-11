
![](images/oauth2/Untitled-fd497460-61c4-4965-8a17-b8713a4c9372.png)

只要是接触过开放各种开放平台的开发者，对于OAuth概念肯定不陌生。但是由于OAuth流程比较复杂，对于刚接触的人来说，容易云里雾里。我之前工作上接触OAuth比较多，本文以OAuth2.0的RFC文档为基础，结合自己以前一些工作上的经验，系统地梳理一下OAuth2.0规范。

## What is OAuth

关于OAuth的定义，维基百科是这么说的：

> OAuth is an open standard for access delegation, commonly used as a way for Internet users to grant websites or applications access to their information on other websites but without giving them the passwords.[1] This mechanism is used by companies such as Amazon,[2] Google, Facebook, Microsoft and Twitter to permit the users to share information about their accounts with third party applications or websites.

O == Open, Auth == Authorization（授权）, not Authentication（认证）.

首先要明确的是，OAuth是一种授权协议，而非认证协议。通过它，用户可以授权第三方应用访问自己保存在资源服务器器上的资源。当然，如果这些资源是账号信息，第三方服务器也可以基于OAuth实现类似SSO的单点登录，完成登录认证。

## OAuth历史

![](images/oauth2/Untitled-e8759fb4-4901-449a-aea8-9abb58109b82.png)

上面这张图基本涵盖了OAuth诞生的相关历史进程。

在2006年，Twitter在开发他们自己的OpenID实现，而当时Ma.gnolia网站需要一个使用OpenID授权访问他们网站资源的方案，双方会面讨论后发现当时并没有一个统一的标准API实现这件事。

上面功能的实现者们于2007年成立了OAuth讨论组，撰写并公布了最早的开放授权（OAuth）草案。这个草案后来得到了Google的关注，最终也一起参与了规范的制定。

在2007年10月，OAuth1.0最后草案公布。

在2008年11月的IETF第73次会议上，OAuth得到广泛支持，IETF正式为它成立了一个工作组。

2010年，编号为RFC 5849的OAuth1.0 RFC文档发表。

在2012年，OAuth2.0 的RFC 6749, 和B earer Token 的 RFC 6750相继发表。大多数互联网应用都以此作为授权标准。需要注意的是OAuth2.0与OAuth1.0并不兼容。

虽然IETF的RFC意为征求意见稿(Request for Comment)，但是目前它已经是开放授权的事实标准。

本文后续的一些内容，基本提炼自IETF的RFC文档。

## 一些概念

了解OAuth2.0之前，我们先熟悉几个概念。

### 角色

![](images/oauth2/Untitled-3328dbda-4800-4e0a-8e69-9aeccec4061d.png)

OAuth2.0 把整个流程中的参与者分为4种角色：

1. Resource Owner：资源拥有者，通常是我们网站/应用的用户。
2. Resource Server：资源服务器，Resource Woner的资源就存储在Resource Server。比如用户在Facebook上存有相册，此时Facebook的相册服务器就是Resource Server.
3. Client：客户端，一般指第三方应用程序，即资源使用方。比如豆瓣注册时，需要用户的微信头像做豆瓣头像，此时豆瓣就是Client。
4. Authorization Server：授权服务器，对Client进行授权时验证客户端，用户合法性的节点。Resource Server 和 Authorization Server可能是同一个（比如资源是账号数据时）也可能不同。

### 几个术语

首先，Client 想要得到Authorization Server 的授权，需要先注册。比如各种开放平台，需要先由开发者提供网站地址，应用名称，默认重定向地址等信息，才能为其颁发合法的Client id 和 Client Secret 进行OAuth授权。

1. Client id：是 Client 在Authorization Server注册的标志，格式各家实现不同，但是需要全局唯一。一般注册后不会改变，也有实现方喜欢叫App id.
2. Client secret：与Client id 配对的密钥，格式各家实现不用，保证完全性即可。在进行OAuth授权流程时，Client必须提供Client id与 Client secret。如果Client secret发生泄露，处于安全考虑，Authorization Server一般允许注册方重新生成secret.
3. User-Agent：一般指用户浏览器，或者APP。
4. Access token：是完成授权流程后，Client得到的票据，访问Resource Owner的资源时，需要对其进行验证。认证失败Authorization Server将引导Client重新进行OAuth流程。
5. Refresh token：类似 AccessToken 的票据，用于刷新Access token（不需要重新走OAuth流程）。Refresh token 是可选项，不一定要实现。

熟悉这些概念后，我们开始介绍OAuth2.0定义的标准授权流程。

## OAuth2.0 Flow

以下几种OAuth Flow，摘选自RFC相关文档，详情请参考最后引用链接。

为覆盖各种场景，OAuth2.0划分了4种授权流程：

1. Authorization Code：授权码模式，因为需要在各个节点往返三次，俗称3 leg。
2. Implicit：隐式授权，相对于授权码模式做了简化。 
3. Resource Owner Password Credentials：密码认证模式。
4. Client Credentials：客户端认证模式。

下面详细介绍这几种模式。

### Authorization Code Grant

下图描述了一个完整的 Authorization Code 模式授权流程，Client与其他角色的交互通过User-Agent，这里 Client 包含前端和后端服务器。

         +----------+
         | Resource |
         |   Owner  |
         |          |
         +----------+
              ^
              |
             (B)
         +----|-----+          Client Identifier      +---------------+
         |         -+----(A)-- & Redirection URI ---->|               |
         |  User-   |                                 | Authorization |
         |  Agent  -+----(B)-- User authenticates --->|     Server    |
         |          |                                 |               |
         |         -+----(C)-- Authorization Code ---<|               |
         +-|----|---+                                 +---------------+
           |    |                                         ^      v
          (A)  (C)                                        |      |
           |    |                                         |      |
           ^    v                                         |      |
         +---------+                                      |      |
         |         |>---(D)-- Authorization Code ---------'      |
         |  Client |          & Redirection URI                  |
         |         |                                             |
         |         |<---(E)----- Access Token -------------------'
         +---------+       (w/ Optional Refresh Token)

1. 步骤A：用户在通过User-Agent(浏览器)使用Client时，Client需要访问用户Resource Owner的资源，此时发起了OAuth流程。Client携带客户端认证信息（Client id 和 Secret）、请求资源的范围、本地状态，重定向地址等重定向到Authorization Server，用户看到授权确认页面。
2. 步骤B：用户认证并确认授权信息，Authorization Server判断用户是否合法来进行下一步授权或者返回错误。
3. 步骤C：如果用户合法且同意授权，Authorization Server使用第一步Client提交的重定向地址重定向浏览器，并携带授权码和之前Client提供的本地状态信息。
4. 步骤D：Client 使用授权码找Authorization Server交换access token(处于安全性考虑，一般由Client 的服务端发起)，为了严格验证，这一步除了携带授权码，还需要前面使用的重定向地址。
5. 步骤E：Authorization Server 验证Client提交的授权码是否有效，重定向地址是否与步骤C匹配。如果验证通过，将返回access token和refresh token（可选）给Client。

得到 access token后，Client可以在token失效前，访问Resource Server得到已授权的用户资源。OAuth2.0在Client与Resource Server之间，设置了一个授权层（authorization layer），Client 通过得到的授权令牌访问资源，对于资源访问权限、时效在颁发令牌时控制。



流程中几个步骤涉及到的接口：

重定向授权页（步骤A）

请求例子：

    GET /authorize?response_type=code&client_id=s6BhdRkqt3&state=xyz&redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb HTTP/1.1
    Host: server.example.com

参数说明：

| Parameter     |Description      |
| :--- | :--- |
|response_type|表示授权类型，必选项，此处的值固定为"code"|
|client_id|表示客户端的ID，必选项|
|redirect_uri|表示重定向URI，可选项。如果不提供，Authorization Server会使用Client注册时的重定向URI进行重定向。|
|scope|表示申请的权限范围，可选项，多个scope值用空格分开|
|state|表示客户端的当前状态，可以指定任意值，认证服务器会原封不动地返回这个值。建议使用。|

重定向回Client（步骤C）

请求例子：

    HTTP/1.1 302 Found
    Location: https://client.example.com/cb?code=SplxlOBeZQQYbYS6WxSbIA&state=xyz

参数说明：

| Parameter     |Description      |
| :--- | :--- |
|code|表示授权码，必选项。该码的有效期应该很短，通常设为10分钟，客户端只能使用该码一次，否则会被授权服务器拒绝。该码与客户端ID和重定向URI，是一一对应关系。|
|state|表示客户端的当前状态，可以指定任意值，认证服务器会原封不动地返回这个值。建议使用。|

从 Authorization Server 获取token（步骤D）

请求例子：

    POST /token HTTP/1.1
    Host: server.example.com
    Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
    Content-Type: application/x-www-form-urlencoded
    
    grant_type=authorization_code&code=SplxlOBeZQQYbYS6WxSbIA&redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb

参数说明：

| Parameter     |Description      |
| :--- | :--- |
|grant_type|表示使用的授权模式，必选项，此处的值固定为"authorization_code"。|
|code|表示上一步获得的授权码，必选项。|
|redirect_uri|表示重定向URI，必选项，且必须与A步骤中的该参数值保持一致。|
|client_id|Client在Authorization Server注册后得到的client_id，必选项。|


Authorization Server 返回token（步骤E）

响应结果例子：

```json
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Cache-Control: no-store
Pragma: no-cache
{
   "access_token":"2YotnFZFEjr1zCsicMWpAA",
   "token_type":"example",
   "expires_in":3600,
   "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
   "example_parameter":"example_value"
}
```

参数说明：

| Parameter     |Description      |
| :--- | :--- |
|access_token|表示访问令牌，必选项。|
|token_type|表示令牌类型，该值大小写不敏感，必选项，可以是bearer类型或mac类型。|
|expires_in|表示过期时间，单位为秒。如果省略该参数，必须其他方式设置过期时间。|
|refresh_token|表示更新令牌，用来获取下一次的访问令牌，可选项。|
|scope|表示权限范围，如果与客户端申请的范围一致，此项可省略。|

### Implicit Grant

Implicit 授权的流程如下图，与 Authorization Code 相比，少了返回授权码这一步，Authorization Server直接返回token至Client的前端，Client方面没有后端参与。图中的Web-Hosted Client Resource可以认为是Client的前端资源容器，比如前端服务器，APP等。

    	 +----------+
         | Resource |
         |  Owner   |
         |          |
         +----------+
              ^
              |
             (B)
         +----|-----+          Client Identifier     +---------------+
         |         -+----(A)-- & Redirection URI --->|               |
         |  User-   |                                | Authorization |
         |  Agent  -|----(B)-- User authenticates -->|     Server    |
         |          |                                |               |
         |          |<---(C)--- Redirection URI ----<|               |
         |          |          with Access Token     +---------------+
         |          |            in Fragment
         |          |                                +---------------+
         |          |----(D)--- Redirection URI ---->|   Web-Hosted  |
         |          |          without Fragment      |     Client    |
         |          |                                |    Resource   |
         |     (F)  |<---(E)------- Script ---------<|               |
         |          |                                +---------------+
         +-|--------+
           |    |
          (A)  (G) Access Token
           |    |
           ^    v
         +---------+
         |         |
         |  Client |
         |         |
         +---------+

1. 步骤A：与 Authorization Code流程类似，Client携带客户端认证信息（Client id 和 Secret）、请求资源的范围、本地状态，重定向地址等重定向到Authorization Server，用户看到授权确认页面。
2. 步骤B：用户认证并确认授权信息，Authorization Server判断用户是否合法来进行下一步授权或者返回错误。
3. 步骤C：如果用户合法且同意授权，Authorization Server使用第一步Client提交的重定向地址重定向浏览器，并将token携带在URI Fragment中一并返回。
4. 步骤D：User-Agent 顺着重定向指示向Web-Hosted Client Resource 发起请求（按RFC2616该请求不包含Fragment）。User-Agent 在本地保留Fragment信息。
5. 步骤E：Web-Hosted Client Resource 返回一个网页（通常是带有嵌入式脚本的HTML），该网页能够提取URI中的Fragment和其他参数。
6. 步骤F：在User-Agent中使用上一步提供的脚本提取URL中的token。
7. 步骤G：User-Agent传送token给Client。

Implicit 比起 Authorization Code 来说，少了Client使用授权码换Token的过程，而是直接把token提供给User-Agent让Client提取。整个流程中使用URL传递token，不需要Client的服务端参与，安全性欠佳（比如某些网络中转节点可以记录URL日志，导致token泄露等）。使用这个方式授权，需要在安全性和便利性之间做好权衡。

流程中几个步骤涉及到的接口：

重定向授权页（步骤A）

请求例子：

    GET /authorize?response_type=token&client_id=s6BhdRkqt3&state=xyz&redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb HTTP/1.1
    Host: server.example.com

参数说明：

| Parameter     |Description      |
| :--- | :--- |
|response_type|表示授权类型，此处的值固定为"token"，必选项。|
|client_id|表示客户端的ID，必选项|
|redirect_uri|表示重定向URI，可选项。如果不提供，Authorization Server会使用Client注册时的重定向URI进行重定向。|
|scope|表示申请的权限范围，可选项，多个scope值用空格分开|
|state|表示客户端的当前状态，可以指定任意值，认证服务器会原封不动地返回这个值。|

携带token重定向回Client（步骤C）

请求例子：

    HTTP/1.1 302 Found
    Location: http://example.com/cb#access_token=2YotnFZFEjr1zCsicMWpAA&state=xyz&token_type=example&expires_in=3600

参数说明：

| Parameter     |Description      |
| :--- | :--- |
|access_token|表示访问令牌，必选项。|
|token_type|表示令牌类型，该值大小写不敏感，必选项，可以是bearer类型或mac类型。|
|expires_in|表示过期时间，单位为秒。如果省略该参数，必须其他方式设置过期时间。|
|refresh_token|表示更新令牌，用来获取下一次的访问令牌，可选项。|
|scope|表示权限范围，如果与客户端申请的范围一致，此项可省略。|


### Resource Owner Password Credentials Grant

这种授权方式其实是常见的用户名密码认证方式。使用这种授权的Client必须是高度可信的，比如操作系统或者高权限的应用。只有当其他的流程不能使用时，才启用这种方式，同时Authorization Server必须特别关注Client确保不会出现安全问题。整个过程中，Client不得保存用户的密码（只能由Client来保证，所以Client必须是高度可信的）。

    	 +----------+
         | Resource |
         |  Owner   |
         |          |
         +----------+
              v
              |    Resource Owner
             (A) Password Credentials
              |
              v
         +---------+                                  +---------------+
         |         |>--(B)---- Resource Owner ------->|               |
         |         |         Password Credentials     | Authorization |
         | Client  |                                  |     Server    |
         |         |<--(C)---- Access Token ---------<|               |
         |         |    (w/ Optional Refresh Token)   |               |
         +---------+                                  +---------------+

1. 步骤A：resource owner 提供给Client用户名密码。
2. 步骤B：Client直接使用用户名密码向Authorization Server进行认证，并请求token。
3. 步骤C：Authorization Server认证Client信息和用户名密码，验证通过后返回token。

流程中几个步骤涉及到的接口：

Client提交用户名密码请求token（步骤B）

请求例子：

    POST /token HTTP/1.1
    Host: server.example.com
    Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
    Content-Type: application/x-www-form-urlencoded
    
    grant_type=password&username=johndoe&password=A3ddj3w


参数说明：

| Parameter     |Description      |
| :--- | :--- |
|grant_type|表示授权类型，此处的值固定为"password"，必选项。|
|username|表示用户名，必选项。|
|password|表示用户的密码，必选项。|
|scope|表示权限范围，可选项。|

Authorization Server返回token信息（步骤C）

响应例子：

```json
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Cache-Control: no-store
Pragma: no-cache

{
   "access_token":"2YotnFZFEjr1zCsicMWpAA",
   "token_type":"example",
   "expires_in":3600,
   "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
   "example_parameter":"example_value"
}
```


这里的响应参数跟Authorization Code 模式是一样的。

### Client Credentials Grant

该模式是Client 访问实现与Authorization Server约定好的资源。Client以自己的名义，而不是以用户的名义，向Authorization Server进行认证。严格地说，Client Credentials 模式并不属于OAuth框架所要解决的问题。在这种模式中，用户直接向Client注册，Client以自己的名义要求Authorization Server提供服务，其实不存在授权问题。

    	 +---------+                                  +---------------+
         |         |                                  |               |
         |         |>--(A)- Client Authentication --->| Authorization |
         | Client  |                                  |     Server    |
         |         |<--(B)---- Access Token ---------<|               |
         |         |                                  |               |
         +---------+                                  +---------------+

1. 步骤A：Client 向Authorization Server进行身份认证，并请求token。
2. 步骤B：Authorization Server 对 Client信息进行认证，有效则发放token。

流程中几个步骤涉及到的接口：

Client申请token（步骤A）

请求例子：

    POST /token HTTP/1.1
    Host: server.example.com
    Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
    Content-Type: application/x-www-form-urlencoded
    
    grant_type=client_credentials


参数说明：

| Parameter     |Description      |
| :--- | :--- |
|grant_type|表示授权类型，此处的值固定为"client_credentials"，必选项。|
|scope|表示权限范围，可选项。|

这一步Authorization Server 必须验证Client。

Authorization Server返回token信息（步骤B）

响应例子：

```json
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Cache-Control: no-store
Pragma: no-cache

{
   "access_token":"2YotnFZFEjr1zCsicMWpAA",
   "token_type":"example",
   "expires_in":3600,
   "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
   "example_parameter":"example_value"
}
```

这里的响应参数跟Authorization Code 模式也是一样的。

### PKCE(Proof Key for Code Exchange)

随着无服务端移动应用或SPA的流行，IETF针对Implicit授权提出了优化方案，在RFC-6749的四种Flow之外另外定义了一种更安全的PKCE模式（RFC-7636）。
PKCE的流程大概如下:

													 +-------------------+
                                                     |   Authz Server    |
           +--------+                                | +---------------+ |
           |        |--(A)- Authorization Request ---->|               | |
           |        |       + t(code_verifier), t_m  | | Authorization | |
           |        |                                | |    Endpoint   | |
           |        |<-(B)---- Authorization Code -----|               | |
           |        |                                | +---------------+ |
           | Client |                                |                   |
           |        |                                | +---------------+ |
           |        |--(C)-- Access Token Request ---->|               | |
           |        |          + code_verifier       | |    Token      | |
           |        |                                | |   Endpoint    | |
           |        |<-(D)------ Access Token ---------|               | |
           +--------+                                | +---------------+ |

这里引入了几个新的变量：t_m（摘要算法），code_verifier，code_challenge（即图中经过算法t_m计算后得到的t(code_verifier)参数）

1. Client随机生成一串字符并作URL-Safe的Base64编码处理, 结果用作code_verifier。
2. 将这串字符通过SHA256哈希，并用URL-Safe的Base64编码处理，结果用作code_challenge。
3. Client使用把code_challenge，请求Authorization Server，获取Authorization Code。（步骤A）
4. Authorization Server 认证成功后，返回Authorization Code（步骤B）。
5. Client 把Authorization Code 和code_verifier请求Authorization Server，换取Access Token。
6. Authorization Server 返回 token。（步骤D）

**由于中间人不能由code_challenge逆推code_verifier，因此即使中间人截获了code_challenge, Authorization Code等，也无法换取Access Token, 避免了implicit模式的安全问题。**

流程中几个步骤涉及到的接口：

Client重定向授权页（步骤A）

请求例子：

    https://{authorizationServerDomain}/oauth2/default/v1/authorize?client_id=0oabygpxgk9l
    XaMgF0h7&response_type=code&scope=openid&redirect_uri=yourApp%3A%2Fcallback&st
    ate=state-8600b31f-52d1-4dca-987c-386e3d8967e9&code_challenge_method=S256&code_
    challenge=qjrzSW9gMiUgpUvqgEPE4_-8swvyCtfOVvg55o5S_es

response_type，client_id，redirect_uri，scope，state 跟implicit 模式是一样的。重点看下其他几个参数。

参数说明：

| Parameter     |Description      |
| :--- | :--- |
|code_verifier|一串用来加密的 43 位到 128 位的随机字符串。由 A-Z，a-z，0-9，还有符号 -._~ 生成。|
|code_challenge|由 code_verifier 来生成，如果设备支持加密，则加密方式为：BASE64URL-ENCODE(SHA256(ASCII(code_verifier)))。如果不支持，则直接使用 code_verifier。|
|code_challenge_method|生成 code_challenge 所用方法，分为 SHA256 和 plain。前者是指 SHA256 方法加密生成，后者是指直接使用 code_verifier，即不加密。|

Authorization Server返回token信息（步骤B）

响应例子：

```json
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Cache-Control: no-store
Pragma: no-cache

{
   "access_token":"2YotnFZFEjr1zCsicMWpAA",
   "token_type":"example",
   "expires_in":3600,
   "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
   "example_parameter":"example_value"
}
```

这里的响应参数跟Authorization Code 模式也是一样的。

## Token

对于token（Access Token和Refresh Token）需要使用什么样的格式，其实没有硬性要求，不同平台有不同的实现方式。这里列举两种常见的token规范，Bearer Token和JWT。

### Bearer Token

OAuth 诞生时就已经定义了两种token格式：Bearer Token 和 Mac Token，Mac 主要使用在无https的环境下，由于OAuth2.0已经要求所有参与者必须使用HTTPS，所以Mac格式不在我们今天讨论范围。Bearer Token由RFC-6750定义。

Bearer Token 格式用BNF范式表示就是：

    b64token    = 1*( ALPHA / DIGIT / "-" / "." / "_" / "~" / "+" / "/" ) *"="
    credentials = "Bearer" 1*SP b64token

换成程序员比较容易理解的正则表达式就是：

    b64token = [0-9a-zA-Z-._~+/]+=
    credentials =  Bearer\s([0-9a-zA-Z-._~+/]+=)+

所以所谓的Bearer Token就是以数字、大小写字母、破折号、小数点、下划线、波浪线、加号、正斜杠、等号结尾组成的Base64编码字符串。在HTTP传输过程中，需要以'Bearer '作为前缀标识。

**Bearer Token 的三种传输方式**

RFC-6750定义了三种传输Bearer Token 的方式，优先级依次递减：

**Authorization Request Header Field（使用HTTP Header的Authorization字段传递）**

    GET /resource HTTP/1.1
    Host: server.example.com
    Authorization: Bearer mF_9.B5f-4.1JqM

**Form-Encoded Body Parameter（使用表单参数传递）**

    POST /resource HTTP/1.1
    Host: server.example.com
    Content-Type: application/x-www-form-urlencoded
    
    access_token=mF_9.B5f-4.1JqM

**URI Query Parameter（使用URI参数传递）**

    GET /resource?access_token=mF_9.B5f-4.1JqM HTTP/1.1
    Host: server.example.com

由于Cookie容易被CSRF攻击，不建议采用cookie的方式传输token。
尽量不要用URI参数的方法，因为浏览器历史记录、服务器日志等可能泄露URI上的机密信息。

### JWT

JWT（JSON Web Token）是近几年移动端常用的token，它可以直接将一些信息编码传递，对客户端更友好。使用JWT有以下有点：

1. 验证token 不需要另外的缓存或者数据库，通过约定好的加密方式解密就行。
2. 因为json的通用性，所以JWT是可以进行跨语言支持的，像JAVA,JavaScript,NodeJS,PHP等很多语言都可以使用。
3. 因为有了payload部分，所以JWT可以在自身存储一些其他业务逻辑所必要的非敏感信息。
便于传输，jwt的构成非常简单，字节占用很小，所以它是非常便于传输的。
4. 它不需要在服务端保存会话信息, 所以它易于应用的扩展。

使用JWT也必须注意一些问题：

1. 不应该在jwt的payload部分存放敏感信息，因为该部分是客户端可解密的部分。
2. 保护好secret私钥，该私钥非常重要。
3. 如果可以，请使用https协议传递JWT。

JWT也有自己的RFC规范RFC-7519，这里简单介绍一下它的格式。详细请参考文末的RFC链接

![](images/oauth2/Untitled-2a5aa9d0-2e52-43d2-9a74-ef24c45f72ac.png)

JWT的格式很简单，一个JWT字符串分为Header，Payload，Signature三部分，他们的原始字符串经过编码后由小数点分隔连接起来：

Header记录着token类型和摘要算法，这里的明文最后要经过Base64URL编码：

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Payload记录着业务信息和用户数据（非敏感），字段可以根据需求自定义。这里的明文同样也要经过Base64URL编码：

```json
{
  "sub": "1234567890",
  "name": "John Doe",
  "admin": true
}
```

Signature是加密后的Header和Payload信息，加密算法需要同Header中alg属性一致，这里是HS256。secret是加密需要的密钥，不可泄漏：

```json
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret)
```

连接编码后的三个部分，就得到一个JWT字符串：

    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.rSWamyAYwuHCo7IFAgd1oRpSP7nzL7BF5t7ItqpKViM

所以当Server端颁发JWT后，Client就可以根据约定好的secret，摘要算法验证Signature并提取Payload信息。

## OAuth 面临的安全问题

OAuth2.0 作为一个授权协议，安全问题尤为重要。OAuth大规模应用的这些年来，主要的安全问题可以分为以下几类：

1. Client Authentication（客户端错误认证），作为Client的开发者，必须保护好自己的client_id client_secret，谨防盗用。
2. Code or Token Steal（票据窃取），OAuth 是票据协议，无法区分使用票据的人是否合法。所以作为Authorization Server，必须对token的失效机制做好控制(如合理的失效时间，限制Code只能用一次，允许用户管理自己已授权的token)。作为Client，必须确保用户授权的token不被采集（最常见的问题就是在log中记录access token）
3. Cross-Site Request Forgery(CSRF攻击)，Authorization Code Grant模式流程较长，存在CSRF隐患。
4. Authorization Code Redirection URI Manipulation（重定向地址篡改），重定向地址篡改是钓鱼网站常用的攻击手段。

前面两种是任何认证授权系统都需要考虑的安全问题，这里重点介绍下后面两种跟OAuth流程比较相关的安全问题。

### CSRF攻击

在OAuth2.0流程中实施CSRF攻击的流程如下：

![](images/oauth2/Untitled-4dc8306d-39bc-4d5d-937c-2e4ae4645294.png)

**原理**

1. 攻击者预先准备好使用自己账号授权生成的 authorization code 的回调地址，引诱用户点击。
2. 用户点击后，变成使用攻击者的账号完成Oauth 流程得到token。
3. 在的第三方app绑定账号的场景，攻击者就可以使用自己的账号完成OAuth登陆用户的第三方app。

**防范措施**

要防止这样的攻击其实很容易，使用RFC规范中推荐的state参数即可，但是由于增加了开发工作量，很多开发者使用OAuth2.0时，经常忽略这个参数。具体细节如下：

1. 在 Authorization Code Grant或者implicit Grant流程的第一步，调用/authorize接口时，带上state参数，state的值由Client指定，生成规则需保证足够随机又有一定业务含义，他人无法轻易假冒。
2. Client 需要保存state参数。
3. 在Authorization Server 认证成功重定向回Client时，会将state原样带回，此时Client需要验证state参数是否一致。

### Authorization Code 流程重定向地址篡改

![](images/oauth2/Untitled-c5f08367-0a87-4b30-a497-d1194581e665.png)

对重定向地址检查也是一个时常被忽略的安全弱点。

**原理**

1. 对于一个正常的第三方Client应用A，攻击者自己也作为一个Client，伪造一个应用A的/authorize请求的链接，其中redirect_uri指向的是攻击者的Client。
2. 攻击者诱导用户点击伪造的链接，发起OAuth2.0的Authorization Code Grant流程。
3. 用户完成认证后，Authorization Server 携带Code重定向回攻击者Client。
4. 攻击者准备一个自己的Code，将上一步应用A的Code替换，伪造一条应用A的回调请求返回给应用A。（此时Code被替换成了攻击者的Code）
5. 应用A的Client在不清楚Code被替换的情况下，继续完成Authorization Code Grant流程，使用攻击者的Code换取Access Token。

此时用户走完OAuth流程，但是在应用A上得到的却是攻击者帐号的授权。大家会觉得，这样有什么问题，又不是用户的授权泄漏。这种攻击方式可以针对绑定帐号的场景，比如用户本来要将豆瓣帐号与微博帐号关联，使用微博的OAuth授权来登陆豆瓣。而被这样钓鱼以后，自己的豆瓣帐号绑定的是攻击者的微博帐号，此时攻击者就可以用他的微博帐号登陆用户的豆瓣帐号了。

**防范方法**

1. Client 注册时，需要开发者提供域名与Client绑定。
2. Authorization Server对/authorize 接口验证的redirect_uri 参数验证，确认与Client注册时提供的域名一致。
3. Authorization Server对/access_token 接口的redirect_uri 参数进行验证，保证与Client发起 /authorize请求时的redirect_uri一致。
4. 对于Authorization Server的Code换token接口，可以要求Client提供client_id和secret，校验此时的code是否产生自同一个client_id。

对于上面提到的CSRF和钓鱼攻击，Client方面如果增加一些授权成功后的提示给用户（比如平台成功与xxx帐号绑定），可以避免用户无意识地授权的情况发生。上面的例子只是简单展示了OAuth授权中需要开发者关注的安全细节，关于OAuth安全想要了解更多，可以参考文末的OAuth安全指南。

## 小结

1. OAuth2.0 规范将参与者划分为 Resource Owner，Resource Server，Client，Authorization Server四种角色。
2. RFC-6749定义了四种OAuth2.0 Grant Flow：Authorization Code Grant，Implicit Grant，Resource Owner Password Credentials Grant，Client Credentials Grant。其中前两种是比较常用的OAuth2.0授权模式。
3. 对于移动端APP或者SPA应用，可以考虑使用PKCE模式减少Implicit Grant的安全风险。
4. 对于Token的格式，建议使用Bearer Token或者JWT。
5. 由于OAuth2.0的Flow步骤较长，不管是Client端还是Authorization Server端，在使用OAuth2.0的时候，最好严格按照RFC规范执行，可以最大程度地减少安全隐患。同时也要注意业界关于OAuth漏洞的披露，及时修复漏洞。

## 参考链接

1. OAuth2.0: [https://oauth.net/2/](https://oauth.net/2/)
2. OAuth2.0(RFC-6749): [https://tools.ietf.org/html/rfc6749](https://tools.ietf.org/html/rfc6749)
3. PKCE(RFC-7636): [https://tools.ietf.org/html/rfc7636](https://tools.ietf.org/html/rfc7636)
4. Bearer token(RFC-6750): [https://tools.ietf.org/html/rfc6750](https://tools.ietf.org/html/rfc6750)
5. JWT(RFC-7519): [https://tools.ietf.org/html/rfc7519](https://tools.ietf.org/html/rfc7519)
6. 乌云平台（备份）OAuth安全指南: [http://drops.xmd5.com/static/drops/papers-1989.html](http://drops.xmd5.com/static/drops/papers-1989.html)
