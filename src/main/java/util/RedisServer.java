package util;

import redis.clients.jedis.Jedis;

/**
 * 获取连接
 * Created by MelloChan on 2017/12/2.
 */
public class RedisServer {
    private static class ResourceHolder {
        public static Jedis jedis = new Jedis("localhost");
    }

    public static Jedis getInstance() {
        return ResourceHolder.jedis;
    }
}
