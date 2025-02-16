package com.pandaq.rxpanda.requests.okhttp.base;

import android.text.TextUtils;

import com.pandaq.rxpanda.RxPanda;
import com.pandaq.rxpanda.api.Api;
import com.pandaq.rxpanda.observer.ApiObserver;
import com.pandaq.rxpanda.requests.Request;
import com.pandaq.rxpanda.transformer.CastFunc;
import com.pandaq.rxpanda.transformer.RxScheduler;
import com.pandaq.rxpanda.utils.CastUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import okhttp3.ResponseBody;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by huxinyu on 2019/1/11.
 * Email : panda.h@foxmail.com
 * <p>
 * Description : baseRequest for use okHttp lib, also can return an observable response
 */
public abstract class HttpRequest<R extends HttpRequest> extends Request<R> {

    // http api,兼容 rxJava 观察者模式，需要返回观察对象时，将请求转换成 Retrofit 去请求
    protected Api mApi;
    protected String url = "";
    // request tag
    protected Object tag;
    protected Map<String, String> localParams = new LinkedHashMap<>();//请求参数
    private long retryDelayMillis;//请求失败重试间隔时间

    /**
     * set request‘s tag，use to manage the request
     *
     * @param tag request's tag
     * @return Request Object
     */
    public R tag(@NonNull Object tag) {
        this.tag = tag;
        return CastUtils.cast(this);
    }

    public HttpRequest(String url) {
        if (!TextUtils.isEmpty(url)) {
            this.url = url;
        }
    }

    public <T> Observable<T> request(Type type) {
        injectLocalParams();
        return execute(type);
    }

    public void  request(ApiObserver callback) {
        injectLocalParams();
        execute(callback);
    }

    protected abstract <T> Observable<T> execute(Type type);

    protected abstract void execute(ApiObserver callback);

    /**
     * 添加请求参数
     *
     * @param paramKey
     * @param paramValue
     * @return
     */
    public R addParam(String paramKey, String paramValue) {
        if (paramKey != null && paramValue != null) {
            this.localParams.put(paramKey, paramValue);
        }
        return CastUtils.cast(this);
    }

    /**
     * 添加请求参数
     *
     * @param params
     * @return
     */
    public R addParams(Map<String, String> params) {
        if (params != null) {
            this.localParams.putAll(params);
        }
        return CastUtils.cast(this);
    }

    /**
     * 移除请求参数
     *
     * @param paramKey
     * @return
     */
    public R removeParam(String paramKey) {
        if (paramKey != null) {
            this.localParams.remove(paramKey);
        }
        return CastUtils.cast(this);
    }

    /**
     * 设置请求参数
     *
     * @param params
     * @return
     */
    public R params(Map<String, String> params) {
        if (params != null) {
            this.localParams = params;
        }
        return CastUtils.cast(this);
    }

    @Override
    protected void injectLocalParams() {
        super.injectLocalParams();
        RxPanda.getRetrofitBuilder().client(builder.build());
        retrofit = RxPanda.getRetrofitBuilder().build();
        if (mGlobalConfig.getGlobalParams() != null) {
            localParams.putAll(mGlobalConfig.getGlobalParams());
        }
        if (retryDelayMillis <= 0) {
            retryDelayMillis = mGlobalConfig.getRetryDelayMillis();
        }
        mApi = retrofit.create(Api.class);
    }

    protected <T> ObservableTransformer<ResponseBody, T> httpTransformer(final Type type) {
        return apiResultObservable -> apiResultObservable
                .compose(RxScheduler.retrySync())
                .map(new CastFunc<T>(type));
    }
}
