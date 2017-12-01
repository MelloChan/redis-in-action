# redis-in-action

系统化学习redis  

### redis数据结构

字符串:    

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

![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/string.PNG)  
  
列表(和LinkedList差不多):  
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
    <th>获取散列包含的所以键值对</th>
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