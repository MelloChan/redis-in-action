## list

redis的list(同时发布与订阅、慢查询、监视器等功能也使用到了链表)底层就是一个双向链表,查阅[adlish.h](https://github.com/antirez/redis/blob/unstable/src/adlist.h):    

![list](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/list-code.png)

由上面的结构体可知:   
1.listNode,链表结点有前置指针、后置指针以及当前节点值组成;  
2.list,该结构体持有链表,head指向头结点、tail指向尾结点、len表明节点数、dup用于复制节点、free用于释放,最后match用于对比链表节点值是否与输入值相等.    

优点:  
1.访问节点前置后置节点的时间复杂度为O(1);  
2.双向链表,获取头尾节点时间复杂度为O(1);  
3.保证无环;   
4.保存了节点数量,获取节点数时间复杂度为O(1);  
5.多态性,节点值使用void*指针保存,可用于保存各种不同类型的值.  


