# redis-in-action

系统化学习redis:       
1.redis实战  
2.redis设计与实现

## 1. 初识redis

### 1.1 redis简介
redis:一个速度非常快的单线程NoSQL DB,可存储键与五种不同类型的值之间的映射.并
且可以将存储在内存中的键值对持久化到硬盘,可以使用复制特性来扩展读性能,使用客户端
分片来扩展写性能.既可作主存储也可为辅助数据库.支持复制/持久化/事务等手段保证数据完整性.  

memcached:一款多线程高性能的键值缓存服务器(字符串类型),与redis性能几近,但redis能提供更加
丰富的数据结构类型且能持久化数据.  

两种持久化方法:①时间点转储 RDB "指定时间段内有指定数量的写操作执行"或"调用转储命令执行";
②AOF 将所有修改了数据库的命令都写入一个只追加文件里面,使用者根据重要程度将只追加写入设置为
从不同步/每秒同步一次/写入一个命令即同步  
前者是默认持久化方式.  

主从复制:执行复制的从数据库会连接上主数据库,接收主数据库的整个初始副本.每当主数据库执行
写命令时,都会被发送给所有连接着的从数据库执行,从而实时更新数据库数据集.避免对主数据库的
集中式访问.   

源码:redis是完全开源的.源码直接参阅-> https://github.com/antirez/redis

### 1.2 redis数据结构
[Java-demo](https://github.com/MelloChan/redis-in-action/tree/master/src/main/java/ch00)  

[字符串](https://github.com/MelloChan/redis-in-action/blob/master/content/sds.md):    

<table>
<tr>
	<th>命令</th>
	<th>行为</th>
</tr>
<tr>
	<th>GET</th>
	<th>获取存储在给定键中的值</th>
</tr>

<tr>
	<th>SET</th>
	<th>设置存储在给定键的值</th>
</tr>

<tr>
	<th>DEL</th>
	<th>删除存储在给定键中值(通用命令)</th>
</tr>
</table>  

demo:  
![sds](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/string.png)  
  
[列表](https://github.com/MelloChan/redis-in-action/blob/master/content/list.md):  
<table>
<tr>
	<th>命令</th>
	<th>行为</th>
</tr>
<tr>
	<th>RPUSH[LPUSH]</th>
	<th>将给定值推入列表的右[左]端</th>
</tr>
<tr>
	<th>LRANGE</th>
	<th>获取列表在给定范围上所以值</th>
</tr>
<tr>
	<th>LINDEX</th>
	<th>获取列表在给定位置上的单个元素</th>
</tr>
<tr>
    <th>LPOP[RPOP]</th>
    <th>从列表的左端[右]端弹出一个值,并返回被弹出的值</th>
</tr>
</table>
demo:
  
![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/list.png)

集合(其实就是Java中HashSet):
<table>
<tr>
	<th>命令</th>
	<th>行为</th>
</tr>
<tr>
	<th>SADD</th>
	<th>将给定元素添加到集合</th>
</tr>
<tr>
	<th>SREM</th>
	<th>如果给定元素存在集合中,则移除该元素</th>
</tr>
<tr>
	<th>SISMEMBER</th>
	<th>检查给定元素是否存在于集合中</th>
</tr>
<tr>
    <th>SMEMBERS</th>
    <th>返回集合包含的所有元素</th>
</tr>
</table>
 demo:
 
 ![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/set.png)
 
 散列:
<table>
<tr>
	<th>命令</th>
	<th>行为</th>
</tr>
<tr>
	<th>HSET</th>
	<th>在散列表里关联给定的键值对</th>
</tr>

<tr>
	<th>HGET</th>
	<th>获取指定键的值</th>
</tr>
<tr>
	<th>HGETALL</th>
    <th>获取散列包含的所有键值对</th>
</tr>
<tr>
    <th>HDEL</th>
    <th>给定键存在于散列中,则移除这个键</th>
</tr>
</table>
demo:  

![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/hash.png)

有序集合(按照分值(浮点数)进行排序):
<table>
<tr>
	<th>命令</th>
	<th>行为</th>
</tr>
<tr>
	<th>ZADD</th>
	<th>将一个带有给定分值的成员添加到有序集合</th>
</tr>
<tr>
    <th>ZRANGE</th>
	<th>根据元素在有序排列中所处的位置,从有序集合里面获取多个元素</th>
</tr>
<tr>
	<th>ZRANGEBYSCORE</th>
	<th>获取有序集合在给定分值范围内的所有元素</th>
</tr>
<tr>
    <th>ZREM</th>
    <th>如果给定成员存在于有序集合,则移除该成员</th>
</tr>
</table>

demo:  
  
![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/zset.png)

### 1.3 你好redis

<a href="https://github.com/MelloChan/redis-in-action/blob/master/src/main/java/ch01/VoteServer.java">vote demo<a/>    

## 2. 使用redis构建web应用
