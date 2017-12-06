package ch01;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;
import util.RedisServer;

import java.util.*;

/**
 * 投票服务
 * Created by MelloChan on 2017/12/3.
 */
public class VoteServer {
    private static final int ONE_WEEK_IN_SECONDS = 7 * 86400;
    /*
        ONE_WEEK_IN_SECONDS/200  假设200为文章能被展示的基本票数
     */
    private static final int VOTE_SCORE = 432;
    /*

     */
    private static final int ARTICLES_PER_PAGE = 25;

    public static void main(String[] args) {
        new VoteServer().run();
    }

    public void run() {
        Jedis conn = RedisServer.getInstance();
        conn.select(15);

        String articleId = postArticle(conn, "username", "A title", "www.google.com");
        System.out.println("posted a new article with id: " + articleId);
        System.out.println("hash looks like: ");
        //获取hash表数据
        Map<String, String> articleData = conn.hgetAll("article:" + articleId);
        for (Map.Entry<String, String> entry : articleData.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println();

        articleVote(conn, "other_user", "article:" + articleId);
        String votes = conn.hget("article:" + articleId, "votes");
        System.out.println(" voted for the article, it now has votes: " + votes);
        assert Integer.parseInt(votes) > 1;

        System.out.println("the currently highest-scoring articles are:");
        List<Map<String, String>> articles = getArticles(conn, 1);
        printArticles(articles);
        assert articles.size() >= 1;

        //对文章进行分组
        addGroups(conn, articleId, new String[]{"new-group"});
        System.out.println("We added the article to a new group, other articles include:");
        articles = getGroupArticles(conn, "new-group", 1);
        printArticles(articles);
        assert articles.size() >= 1;
    }

    /**
     * @param conn  当前连接
     * @param user  用户名
     * @param title 标题
     * @param link  链接
     * @return
     */
    public String postArticle(Jedis conn, String user, String title, String link) {
        //增长id
        String articleId = String.valueOf(conn.incr("article:"));

        String voted = "voted:" + articleId;
        // voted:id 某篇文章的唯一标识 自动将发布者添加到文章已投票用户名单
        conn.sadd(voted, user);
        //过期时间设置 一周
        conn.expire(voted, ONE_WEEK_IN_SECONDS);
        // now 时间戳 s
        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        HashMap<String, String> articleData = new HashMap<>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        //设置多个键值对 存储在散列表中
        conn.hmset(article, articleData);
        //设置排序集合 根据评分排序与根据发布时间排序
        conn.zadd("score:", now + VOTE_SCORE, article);
        conn.zadd("time:", now, article);

        return articleId;
    }

    /**
     * 为文章进行投票
     *
     * @param conn    连接
     * @param user    用户名
     * @param article 唯一文章鉴定
     */
    public void articleVote(Jedis conn, String user, String article) {
        long cutoff = (System.currentTimeMillis() / 1000) - ONE_WEEK_IN_SECONDS;
        if (conn.zscore("time:", article) < cutoff) {
            return;
        }

        String articleId = article.substring(article.indexOf(':') + 1);
        //该id首次对某文章进行投票 则进行加分值操作
        if (conn.sadd("voted:" + articleId, user) == 1) {
            //分值自增
            conn.zincrby("score:", VOTE_SCORE, article);
            //更新投票数量
            conn.hincrBy(article, "votes", 1);
        } //这三个操作理论上应该进行事务控制
    }


    public List<Map<String, String>> getArticles(Jedis conn, int page) {
        return getArticles(conn, page, "score:");
    }

    public List<Map<String, String>> getArticles(Jedis conn, int page, String order) {
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;

        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String, String>> articles = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }

        return articles;
    }

    /**
     * 将文章进入指定分组
     * @param conn 连接
     * @param articleId 文章id
     * @param toAdd 分组
     */
    public void addGroups(Jedis conn, String articleId, String[] toAdd) {
        String article = "article:" + articleId;
        for (String group : toAdd) {
            conn.sadd("group:" + group, article);
        }
    }

    /**
     * 获取分组文章
     * @param conn 连接
     * @param group 分类的群组
     * @param page 页数
     * @return 分组列表
     */
    public List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page) {
        return getGroupArticles(conn, group, page, "score:");
    }

    /**
     *
     * @param conn 连接
     * @param group 分类的群组
     * @param page 页数
     * @param order 排序群组key
     * @return
     */
    public List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page, String order) {
        String key = order + group;
        if (!conn.exists(key)) {
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order);
            conn.expire(key, 60);
        }
        return getArticles(conn, page, key);
    }

    /**
     * 打印文章列表
     * @param articles
     */
    private void printArticles(List<Map<String, String>> articles) {
        for (Map<String, String> article : articles) {
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String, String> entry : article.entrySet()) {
                if (entry.getKey().equals("id")) {
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
