package ch00;

import redis.clients.jedis.Jedis;

/**
 * list
 * Created by MelloChan on 2017/12/2.
 */
public class RedisList {
    public static void main(String[] args) {
        Jedis jedis = RedisServer.getInstance();
        jedis.rpush("list-demo", "l2", "l3", "l4");
        jedis.lpush("list-demo", "l1", "l0");
        System.out.println(jedis.lrange("list-demo", 0, -1));
        System.out.println(jedis.lindex("list-demo", 0));
        System.out.println(jedis.lpop("list-demo"));
        System.out.println(jedis.rpop("list-demo"));
        System.out.println(jedis.lrange("list-demo", 0, -1));
        System.out.println(jedis.del("list-demo"));
    }
}
/*
[l0, l1, l2, l3, l4]
l0
l0
l4
[l1, l2, l3]
1
 */