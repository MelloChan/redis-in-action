package ch00;

import redis.clients.jedis.Jedis;
import util.RedisServer;

/**
 * zset
 * Created by MelloChan on 2017/12/2.
 */
public class RedisZset {
    public static void main(String[] args) {
        Jedis jedis= RedisServer.getInstance();
        jedis.zadd("z-demo",100,"mysql");
        jedis.zadd("z-demo",200,"postgresql");
        jedis.zadd("z-demo",10,"redis");
        jedis.zadd("z-demo",50,"mongodb");
        System.out.println( jedis.zrange("z-demo",0,-1));
        System.out.println( jedis.zrangeByScoreWithScores("z-demo",0,200));
    }
}
