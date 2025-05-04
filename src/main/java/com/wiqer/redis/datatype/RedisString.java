package com.wiqer.redis.datatype;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lilan
 */
public class RedisString implements RedisData {
    private volatile long timeout;

    @Setter
    @Getter
    private BytesWrapper value;

    public RedisString(BytesWrapper value) {
        this.value = value;
        this.timeout = -1;
    }

    public RedisString() {

    }

    @Override
    public long timeout() {
        return timeout;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
