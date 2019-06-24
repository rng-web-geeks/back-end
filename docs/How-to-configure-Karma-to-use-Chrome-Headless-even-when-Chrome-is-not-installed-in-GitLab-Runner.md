# 在 GitLab Runner 未安装 Chrome 的情况下，配置 Karma 使用 Chrome Headless 进行单元测试 (踩坑日记)

Howie Chen - 铃盛软件Web Application Team



# 背景

最近在为一个项目配置基于 GitLab Pipeline 的持续集成环境，用于单元测试，整体的工作流是：

1. 每次提交代码后，自动触发一条 GitLab Pipeline 来跑单元测试。
2. 跑完之后，自动把结果发到通讯工具（比如 RingCentral Glip）。



# 初次尝试

要实现这个工作流，首先想到的就是在 Karma 中配置用 PhantomJS 来跑单元测试，但考虑到 PhantomJS 的作者已经停止维护这个项目（[官方通告](https://github.com/ariya/phantomjs/issues/15344)），所以打算用 Chrome Headless 来替换它。

```javascript
// karma.conf.js
module.exports = function(config) {
  config.set({
    browsers: ['PhantomJS']  // change to ['ChromeHeadless']
  })
}
```

对于大多数情况，即机器有全局安装 Chrome 的情况，只要配置到这里就够了，整个工作流可以很顺利地跑起来了。

但目前的困境是，项目所使用的 GitLab Runner 并没有全局安装 Chrome，而且这个 GitLab Runner 是几个项目公用的，不方便直接登录这台 GitLab Runner 去安装。这可如何是好呢？



# 灵光闪现

正在发愁之际，脑子突然灵光闪现：Chrome 不是有一个同胞的好兄弟 [Puppeteer](https://github.com/GoogleChrome/puppeteer) 嘛！安装依赖项 `puppeteer` 时，Puppeteer 会自动下载最新版的 Chrome。这样一来，项目就有了专用的 Chrome，与环境独立开来。说干就干：

1. 添加 `puppeteer` 依赖项

   ```shell
    npm i -D puppeteer
   ```

2. 配置 Karma 使用 Puppeteer 内置的 Chrome

   ```javascript
    // karma.conf.js
    process.env.CHROME_BIN = require('puppeteer').executablePath();
    
    module.exports = function(config) {
      config.set({
        browsers: ['ChromeHeadless']
      })
    }
   ```
   
   > **备注**
   >
   > 1. 在 Karma 中，设置环境变量 CHROME_BIN 可以手动指定 Chrome 可执行文件的路径（[文档](http://karma-runner.github.io/4.0/config/browsers.html)）。
   > 2. executablePath() 是 Puppeteer 官方提供的 Public API（[文档](https://github.com/GoogleChrome/puppeteer/blob/v1.18.0/docs/api.md#puppeteerexecutablepath)）。



# 问题再现

原本以为这么搞，就万事大吉了，没想到还是出幺蛾子了：

    23 06 2019 05:43:24.813:INFO [karma-server]: Karma v4.0.1 server started at http://0.0.0.0:9876/
    23 06 2019 05:43:24.815:INFO [launcher]: Launching browsers ChromeHeadless with concurrency unlimited
    23 06 2019 05:43:24.819:INFO [launcher]: Starting browser ChromeHeadless
    23 06 2019 05:43:24.832:ERROR [launcher]: Cannot start ChromeHeadless
    	/builds/<project-path>/node_modules/puppeteer/.local-chromium/linux-669486/chrome-linux/chrome: error while loading shared libraries: libX11-xcb.so.1: cannot open shared object file: No such file or directory

看来是有什么 Chrome 运行所需要的共享库缺失，查了下 [Puppeteer 官方的 Troubleshooting 页面](https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md#chrome-headless-doesnt-launch-on-unix)，找到了解决方案：**配置 Karma 在启动前，先安装好 Chrome 所依赖的 Shared Linux Package**。这里我的实现方式是修改 `.gitlab-ci.yml`：

```yaml
unit_test_job:
  stage: test
  script:
    - node -v
    - npm -v
    - apt-get update
    - apt-get -y install gconf-service libasound2 libatk1.0-0 libatk-bridge2.0-0 libc6 libcairo2 libcups2 libdbus-1-3 libexpat1 libfontconfig1 libgcc1 libgconf-2-4 libgdk-pixbuf2.0-0 libglib2.0-0 libgtk-3-0 libnspr4 libpango-1.0-0 libpangocairo-1.0-0 libstdc++6 libx11-6 libx11-xcb1 libxcb1 libxcomposite1 libxcursor1 libxdamage1 libxext6 libxfixes3 libxi6 libxrandr2 libxrender1 libxss1 libxtst6 ca-certificates fonts-liberation libappindicator1 libnss3 lsb-release xdg-utils wget
    - npm install
    - npm run test
```

> **补充说明**
>
> - 只需关注以 apt-get 开头的那两行就行了。
> - apt-get -y install ... 中的参数 -y 指的是：面对可能出现的提示，都自动选择 Yes。



# 问题连连

如果你觉得事情到这儿应该差不多了吧，我也希望是这样，但问题总是接踵而至。请看报错：

    23 06 2019 06:20:41.117:INFO [karma-server]: Karma v4.0.1 server started at http://0.0.0.0:9876/
    23 06 2019 06:20:41.119:INFO [launcher]: Launching browsers ChromeHeadless with concurrency unlimited
    23 06 2019 06:20:41.122:INFO [launcher]: Starting browser ChromeHeadless
    23 06 2019 06:20:41.323:ERROR [launcher]: Cannot start ChromeHeadless
    	[0623/062041.155916:ERROR:zygote_host_impl_linux.cc(89)] Running as root without --no-sandbox is not supported. See https://crbug.com/638180.

问题出现的原因是：GitLab Runner 是以 root 的身份去执行的，在这种情况下 Chrome 必须加上 `--no-sandbox` 的参数。因此要对 `karma.conf.js` 作如下修改：

```javascript
browsers: ['ChromeHeadless_withoutSecurity'],
customLaunchers: {
  ChromeHeadless_withoutSecurity: {
	  base: 'ChromeHeadless',
    flags: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-gpu',
      '--remote-debugging-port=9222'
    ]
  }
}
```

以上这个方案来自 [jmaitrehenry](https://github.com/karma-runner/karma-chrome-launcher/issues/170#issuecomment-374342709)。

除了 `--no-sandbox` 以外，为什么还有另外 3 个参数呢？这个还有待进一步研究。目前知道的是，去掉这 3 个参数之后，Chrome Headless 可以跑起来，但却无法与 Karma 连接。



# 结语

至此，在经历了一番风风雨雨之后，GitLab Pipeline 终于如我所愿地跑起来了。

把这次的经历写成文章，除了作为笔记以外，我还想把它分享给其它遇到类似问题的人，希望与大家有更多的交流，共同进步。



# 参考文献

- [Puppeteer](https://github.com/GoogleChrome/puppeteer)
- [Puppeteer - Chrome headless doesn't launch on UNIX](https://github.com/GoogleChrome/puppeteer/blob/master/docs/troubleshooting.md#chrome-headless-doesnt-launch-on-unix)
- [Karma - Configuration for Browsers](http://karma-runner.github.io/4.0/config/browsers.html)
- [PhantomJS - Archiving the project: suspending the development](https://github.com/ariya/phantomjs/issues/15344)
- [Github Issue - ChromeHeadless (Puppeteer) not captured when running in docker](https://github.com/karma-runner/karma-chrome-launcher/issues/170#issuecomment-374342709)
