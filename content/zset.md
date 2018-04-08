## zset

redis的有序集合数据类型底层主要是跳表,而跳表简单讲是一组有序链表的结合,这些链表分了多级,越往上节点越少,除了底层节点其他节点都有指向下一个以及下一层节点的指针,跳表的查找添加删除平均时间复杂度都在O(logN),是以空间换时间的数据结构( 参考: http://blog.jobbole.com/111731/ ).  

查阅官方源码[server.h](https://github.com/antirez/redis/blob/unstable/src/server.h):  
![zset](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/zset-code.png)  

由上面的结构体可知:  
1.zskiplistNode, 跳表的节点,包含成员字符串、分值、后退指针、层(包含前进指针与跨度);    
2.zskiplist, header指向跳跃表的表头节点、tail指向表尾节点、length代表跳跃表的长度、level代表层数最大的那个节点层数(表头节点不计算在内);    
3.zset, 以上组合构成了redis的有序集合.其内部依靠分值进行排序,成员字符串是唯一的.       
![跳跃表](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/zskiplist.png)