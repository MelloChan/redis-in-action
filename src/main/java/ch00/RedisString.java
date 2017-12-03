package ch00;

import redis.clients.jedis.Jedis;
import util.RedisServer;

/**
 * string
 * Created by MelloChan on 2017/12/2.
 */
public class RedisString {
    public static void main(String[] args) {
        Jedis jedis = RedisServer.getInstance();
        jedis.set("hello","redis");
        System.out.println(jedis.get("hello"));
        jedis.set("hello","world");
        System.out.println(jedis.get("hello"));
        jedis.del("hello");
        System.out.println(jedis.get("hello"));
    }
}/*
redis
world
null
*/
