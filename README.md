
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1a9236f222ac4293a509c9db710a13f5)](https://app.codacy.com/app/PandaQAQ/RxPanda?utm_source=github.com&utm_medium=referral&utm_content=PandaQAQ/RxPanda&utm_campaign=Badge_Grade_Dashboard)  [![License](https://img.shields.io/github/license/PandaQAQ/RxPanda.svg)](https://github.com/PandaQAQ/RxPanda/blob/master/LICENSE)  [![Download](https://api.bintray.com/packages/huxinyu/maven/rxpanda/images/download.svg?version=0.2.0)](https://bintray.com/huxinyu/maven/rxpanda/0.2.0/link)

# RxPanda
基于 `RxJava2` `Retrofit2` `Okhttp3` 封装的网络请求解析一条龙服务库

> - 支持解析数据壳 key 自定义
> - 支持接口单独配置禁用脱壳返回接口定义的原始对象
> - 支持多 host 校验
> - 支持日志格式化及并发按序输出
> - 支持 data 为基本数据类型
> - 支持 int 类型 json 解析为 String 不会 0 变成 0.0
> - 支持基本的文件下载上传功能（不支持断点续传）
### gradle dependencies
```java
    implementation 'com.pandaq:rxpanda:x.x.x'(download tag version)
```

# TODO
> - 1、简单缓存支持

# Release Logs
> - 0.2.0: 使用 LogPrinter 同步输出并发请求日志，避免日志错乱
> - 0.1.9: 兼容 boolean 类型的 data
> - 0.1.8: 兼容 Android 9.0 移除反射方式替换 GsonAdapter，改用注册方式
> - 0.1.7：文件上传下载支持
> - 0.1.6：fix 数字解析为 String 类型时变成 double 类型字符串（1 按 String 解析变为 1.0 bug）

# 基本用法
#### 一、全局配置推荐在 Application 初始化时配置
```java
        RxPanda.globalConfig()
                .baseUrl(ApiService.BASE_URL) //配置基础域名
                .netInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY)) //添加日志拦截器
                .apiSuccessCode(100L) // 数据壳解析时接口成功的状态码
                .hosts("http://192.168.0.107:8080") // 兼容另一个 host（默认只允许基础域名接口访问）
                .connectTimeout(10000) // 连接超时时间（ms）
                .readTimeout(10000) // 读取超时时间（ms）
                .writeTimeout(10000) // 写入超时时间（ms）
                .debug(BuildConfig.DEBUG);// 是否 dubug 模式（非 debug 模式不会输出日志）
```
以上只是精简的配置，还可以通过 GlobalConfig 配置类进行更多的全局配置
#### 二、接口定义
``` kotlin
public interface ApiService {

    // 在线 mock 正常使用 ApiData 数据壳
    @GET("xxx/xxx/xxx")
    Observable<List<ZooData>> getZooList();

    // 数据结构不变但是数据壳 jsonKey 与框架默认不一致时使用此注解，也可在 Config 配置全局使用此数据壳
    @ApiData(clazz = ZooApiData.class)
    @GET("xxx/xxx/xxx")
    Observable<List<ZooData>> newJsonKeyData();

    // 与 ApiData 结构完全不一样使用 RealEntity 标准不做脱壳处理，返回 ZhihuData 就解析为 ZhihuData
    @RealEntity
    @GET("xxx/xxx/xxx")
    Observable<ZhihuData> zhihu();
}
```
与 retrofit 完全一样的基础上增加了两个自定义注解
- 1、 @RealEntity

	接口数据未使用 ApiData 进行数据壳包装，需要直接解析未定义对象时使用。如上面代码中的 `ZhihuData` 在解析时不会进行脱壳操作，接口返回 `ZhihuData` 就解析为 `ZhihuData`
- 2、@ApiData(clazz = ZooApiData.class)

	接口数据使用 ApiData 进行数据壳包装，但包装的 key 与默认的 ApiData 不一致时，可自定义数数据壳实现 IApiData 接口
```kotlin
// 自定义解析 key
data class ZooApiData<T>(
    @SerializedName("errorCode") private val code: Long,
    @SerializedName("errorMsg") private val msg: String,
    @SerializedName("response") private val data: T
) : IApiData<T> {
    override fun getCode(): Long {
        return code
    }

    override fun getMsg(): String {
        return msg
    }

    override fun getData(): T {
        return data
    }

    override fun isSuccess(): Boolean {
        return code.toInt() == 100
    }

}
```
如果全部接口都是按 ZooApiData 的解析 key 格式返回的数据,也不用麻烦的每个接口都加注解。直接在第一步的配置中使用全局配置来配置全局的数据壳

``` kotlin
  .apiDataClazz(ZooApiData::class.java)
```
### 三、请求使用

### Retrofit 方式
``` kotlin
    private val apiService = RxPanda.retrofit().create(ApiService::class.java)
	
				. . .
				
	 apiService.zooList
                    .doOnSubscribe { t -> compositeDisposable.add(t) }
                    .compose(RxScheduler.sync())
                    .subscribe(object : ApiObserver<List<ZooData>>() {
                        override fun onSuccess(data: List<ZooData>?) {
                            // do something
                        }

                        override fun onError(e: ApiException?) {
                            // do something when error
                        }

                        override fun finished(success: Boolean) {
                            // do something when all finish
                        }
                    })
	
				. . .
	
```

### Http 请求方式
此方式直接使用，不需要第二部的接口定义
``` kotlin
	// 以 get 请求为例
    RxPanda.get("https://www.xx.xx.xx/xx/xx/xx")
    .addParam(paramsMap)
    .tag("tags") // 可使用 RequestManager 根据 tag 管理请求
    .request(object :ApiObserver<List<ZooData>>(){
        override fun onSuccess(data: List<ZooData>?) {
            // do something
        }

        override fun onError(e: ApiException?) {
            // do something when error
        }

        override fun finished(success: Boolean) {
            // do something when all finish
        }

    })

```
### 文件上传
```kotlin
        RxPanda.upload("url")
            .addImageFile("key",file)
//            .addBytes("key",bytes)
//            .addStream("key",stream)
//            .addImageFile("key",file)
            .request(object : UploadCallBack() {
                override fun done(success: Boolean) {

                }

                override fun onFailed(e: Exception?) {

                }

                override fun inProgress(progress: Int) {

                }

            })
```
### 文件下载
```kotlin
        RxPanda.download("url")
            .target(file)
//            .target(path,fileName)
            .request(object : UploadCallBack() {
                override fun done(success: Boolean) {

                }

                override fun onFailed(e: Exception?) {

                }

                override fun inProgress(progress: Int) {

                }

            })
```

# proguard-rules
``` java
-keep @android.support.annotation.Keep class * {*;}

-keep class android.support.annotation.Keep

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <init>(...);
}

########### OkHttp3 ###########
-dontwarn okhttp3.logging.**
-keep class okhttp3.internal.**{*;}
-dontwarn okio.**

########### RxJava RxAndroid ###########
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

########### Gson ###########
-keep class com.google.gson.stream.** { *; }
-keepattributes EnclosingMethod
# Gson 自定义相关
-keep class com.pandaq.rxpanda.entity.**{*;}
-keep class com.pandaq.rxpanda.gsonadapter.**{*;}
```