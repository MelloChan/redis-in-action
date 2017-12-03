package ch00;

import redis.clients.jedis.Jedis;
import util.RedisServer;

/**
 * hash
 * Created by MelloChan on 2017/12/2.
 */
public class RedisHash {
    public static void main(String[] args) {
        Jedis jedis = RedisServer.getInstance();
        jedis.hset("hash-demo", "k1", "v1");
        jedis.hset("hash-demo", "k2", "v2");
        jedis.hset("hash-demo", "k3", "v3");
        System.out.println(jedis.hgetAll("hash-demo"));
        System.out.println(jedis.hget("hash-demo", "k1"));
//        jedis.set("hash-demo", "k1", "v11");
//        System.out.println(jedis.hget("hash-demo", "k1"));
        jedis.del("hash-demo", "k1");
        System.out.println(jedis.hgetAll("hash-demo"));
    }
}
