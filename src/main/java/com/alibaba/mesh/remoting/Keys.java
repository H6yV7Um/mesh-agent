package com.alibaba.mesh.remoting;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;

import io.netty.util.AttributeKey;

/**
 * @author yiji
 */
public interface Keys {

    public static final AttributeKey<URL> URL_KEY = AttributeKey.valueOf(Constants.CHANNEL_ATTRIBUTE_URL_KEY);

    public static final AttributeKey<String> SIDE_KEY = AttributeKey.valueOf(Constants.SIDE_KEY);

    public static final AttributeKey<Object> CHARSET_KEY = AttributeKey.valueOf(Constants.CHARSET_KEY);

    public static final AttributeKey<Long> READ_TIMESTAMP = AttributeKey.valueOf(Constants.READ_TIMESTAMP);

    public static final AttributeKey<Long> WRITE_TIMESTAMP = AttributeKey.valueOf(Constants.WRITE_TIMESTAMP);

    public static final AttributeKey<Boolean> READONLY_KEY = AttributeKey.valueOf(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY);

    public static final AttributeKey<Integer> WEIGHT_KEY = AttributeKey.valueOf(Constants.WEIGHT_KEY);

}
