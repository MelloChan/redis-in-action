## string

redis的字符串采用简单动态字符串类型,查阅redis的[sds.h](https://github.com/antirez/redis/blob/unstable/src/sds.h):  

![sds](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/string-code.png)  

由上面的结构体,sds比起C语言的字符串,其提供:  
1.len 指明当前字符串长度;      
2.alloc(free) 表明剩余可用空间;      
3.flags 表明类型,前三位标记类型,后五位作为今后的扩展;     
4.buf 字节数组存储数据.       

优点:  
1.使获取字符串长度时间复杂度为O(1);    
2.有效防止字符串溢出,对字符串的增加操作会先检查是否大于free,是的话扩充buf,并分配未使用空间free=len(少于1MB时,大于直接分配free=1MB);  
3.惰性空间释放策略,字符串缩短时不会主动释放多余空间;  
4.标记使用的字符类型,最大化节省内存;   
5.二进制安全,len来表明字符串长度,而不是'\0',即可存储文本数据也能保持任意的二进制数据.

