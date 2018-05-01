### 事件

Redis服务器是一个事件驱动程序,服务器需要处理以下两类事件:
- 文件事件  
redis服务器通过套接字socket与客户端(或者其他redis服务器)进行连接,而文件事件就是服务器对套接字操作的抽象.服务器与客户端的通信会产生相应的文件事件,而服务器则通过监听并处理这些事件来完成一系列网络通信操作.  

- 时间事件  
redis服务器中的一些操作(比如serverCron函数)需要在给定的时间点执行,而时间时间就是服务器对这类定时操作的抽象.  

#### 文件事件

redis基于Reactor模式开发了自己的网络事件处理器:这个处理器被称为文件事件处理器:      
- 使用I/O多路复用程序来同时监听多个套接字,并根据套接字目前执行的任务来为套接字关联不同的事件处理器  
- 当被监听的套接字准备好执行连接应答、读取、写入、关闭等操作时,与操作相对应的文件事件就会产生,这时文件事件处理器就会调用套接字之前关联好的事件处理器来处理这些事件  

虽然文件事件处理器以单线程方式运行,但通过使用I/O多路复用技术来监听多个套接字,这样既实现了高性能的网络通信模型,又能很好地对接redis其他单线程模块,这保存了redis内部单线程设计的简单性.  

文件事件处理器的构成:  
![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/event-文件事件处理器的组成.png)

I/O多路复用程序的实现:   
![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/event-IO多路复用.png)  

redis的I/O多路复用程序的所有功能都是通过包装常见的select、epoll、evport、kqueue这些I/O多路复用函数库来实现的.在源码中都对应了一个文件:  
![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/event-IO多路复用函数.png)  
redis为这些函数库都实现了相同的API,因此底层实现是可以互换的,通过宏定义了相应的规则,程序在编译时程序会自动选择系统中性能最高的函数库来作为redis的I/O多路复用程序的实现.    
```
#ifdef HAVE_EVPORT
#include "ae_evport.c"
#else
    #ifdef HAVE_EPOLL
    #include "ae_epoll.c"
    #else
        #ifdef HAVE_KQUEUE
        #include "ae_kqueue.c"
        #else
        #include "ae_select.c"
        #endif
    #endif
#endif
```

在[ae.h](https://github.com/antirez/redis/blob/unstable/src/ae.h)文件中定义了事件类型:  
```
#define AE_NONE 0       /* No events registered. */
#define AE_READABLE 1   /* Fire when descriptor is readable. */
#define AE_WRITABLE 2   /* Fire when descriptor is writable. */
#define AE_BARRIER 4    /* With WRITABLE, never fire the event if the
                           READABLE event already fired in the same event
                           loop iteration. Useful when you want to persist
                           things to disk before sending replies, and want
                           to do that in a group fashion. */
```
当套接字可读可写时会优先先读后写.在 [ae.c](https://github.com/antirez/redis/blob/unstable/src/ae.c) 中有各类事件处理(创建/删除/获取/阻塞等)api的具体实现.   
而各类事件处理器则在 [networking.c](https://github.com/antirez/redis/blob/unstable/src/networking.c) 中有具体实现,有连接应答处理器、命令请求处理器、命令回复处理器以及复制处理器.其中最常用的是前三者,复制处理器主要用来处理redis的主从服务器进行复制操作时的处理.    

一次完整的客户端与服务器连接事件:    
![](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/event-通信过程)


  




