## set

redis的集合底层就是一个数组,查阅源码[intset.h](https://github.com/antirez/redis/blob/unstable/src/intset.h): 

![set](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/set-code.png)

1.encoding, 表明保存整数集合类型;  
2.length, 集合元素数量;    
3cntents, 按照从小到大保存整数.  