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
_注意: 如要使用请首先安装TTS服务_

# SDK使用
请参考demo实现 [CameraBase_1_3.java](https://github.com/t2mobile/t2mediaDemo/raw/master/app/src/main/java/com/hf/t2mediademo/CameraBase_1_3.java)

# API文档
见仓库根目录下的doc目录

