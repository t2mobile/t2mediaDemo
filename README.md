# t2mediaDemo
这个Demo用于演示t2media SDK如何使用。t2media实现功能如下：
1. 移动侦测
2. 视频分流
3. 视频水印叠加
4. 视频抽帧
5. TTS (需要安装额外的TTS服务)

# SDK文件
[t2media-release.aar](https://github.com/t2mobile/t2mediaDemo/raw/master/app/libs/t2media-release.aar) <br>
[t2auth-release.aar](https://github.com/t2mobile/t2mediaDemo/raw/master/app/libs/t2auth-release.aar)

# 引用SDK (Android Studio)
```
implementation(name: 't2media-release', ext: 'aar') 
implementation(name: 't2auth-release', ext: 'aar')
```

# 初始化SDK
建议在Application.onCreate()中添加以下初始化代码
```
import com.t2m.media.Initializer;
import com.t2m.tts.Tts; // 如果不需要TTS则无需import

......

Initializer.init(this); // 初始化SDK
Tts.init(this); // 初始化TTS服务, 如果不需要TTS则无需初始化
```
_注意: 如要使用TTS请首先安装TTS服务: [t2tts-release.apk](https://github.com/t2mobile/t2mediaDemo/raw/master/TtsService/t2tts-release.apk)_

# SDK使用
请参考demo实现 [CameraBase_1_3.java](https://github.com/t2mobile/t2mediaDemo/raw/master/app/src/main/java/com/hf/t2mediademo/CameraBase_1_3.java)

# API文档
见仓库根目录下的doc目录

# 关于授权
SDK以及TTS服务均只能运行在C7设备之上

# 已知问题
1. 预览界面偶现异常
    * Demo的已知问题，和SDK无关。会在在后续Demo更新中修复
2. TTS Service crash
    * TTS Service的问题会在后续版本中修复
3. Demo crash
    * Demo的已知问题，和SDK无关。会在在后续Demo更新中修复
4. 分流状态且缓慢
    * Demo在切换分流状态时对Camera做了完全的关闭和开启导致。可通过优化Camera打开关闭的业务逻辑实现提升。Demo中仅做SDK的功能演示，不做这个优化。
