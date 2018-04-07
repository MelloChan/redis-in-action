### hash

redis的字典底层结构与Java的HashMap相似,采用数组+链表的形式,查阅[dict.h](https://github.com/antirez/redis/blob/unstable/src/dict.h):  

![dict](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/dict-code.png)    

由上面的结构体可知:   
1.dictht, 存放节点的table,记录哈希表大小的size,用于计算索引的掩码sitemask(大小总为size-1),记录节点数量的used;    
2.dictEntry, key保存着键,v则保存着值(可以是指针、整数值、浮点数),next指向下一个节点;    
3.dict, type指向dictType结构的指针,该结构保存对键值对的一系列操作函数,privdata为传递的参数,ht保存着哈希表,ht[1]作为重哈希时扩容用;     
4.dictType, 一系列操作键值对的函数,包括计算哈希值、复制key、复制value、对比key、销毁key等.  

哈希算法:取key的哈希值,调用hashFunction函数,然后将 哈希值 & sizemask 计算出索引.  

解决键冲突:链地址法,采用一个单向链表将那些键冲突且不相等的键值对使用next指针连接.

rehash:哈希表的负载由 容量*负载因子 决定,rehash时ht[1]就会被分配内存空间,而出发rehash有两种情况:  
1.扩容,这时ht[1]大小为第一个大于等于ht[0].used*2的2^n(2的n次方幂);  
2.收缩,这时ht[1]大小为第一个大于等于ht[0].used的2^n.  
之后将ht[0]的键值对迁移(这个过程一般是渐进的)到ht[1]上,rehash指的是重新计算键的哈希值和索引值,迁移完毕后将释放ht[0]空间,然后将ht[1]设置为ht[0],并为ht[1]新创建一个哈希表,为下次rehash做准备.  
    
  
  


