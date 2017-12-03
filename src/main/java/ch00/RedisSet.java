package ch00;

import redis.clients.jedis.Jedis;
import util.RedisServer;

/**
 * set
 * Created by MelloChan on 2017/12/2.
 */
public class RedisSet {
    public static void main(String[] args) {
        Jedis jedis= RedisServer.getInstance();
        jedis.sadd("set-demo","s1");
        jedis.sadd("set-demo","s2");
        System.out.println(jedis.smembers("set-demo"));
        System.out.println(jedis.sismember("set-demo","s1"));
        jedis.srem("set-demo","s1");
        System.out.println(jedis.smembers("set-demo"));
    }
}
/*
[s2, s3, s1, s0]
true
[s2, s3, s0]
 */