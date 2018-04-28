### AOF持久化

除了RDB持久化功能之外,Redis还提供了AOF持久化功能.与RDB持久化通过保存数据库中的键值对来记录数据库状态不同,AOF是通过保存Redis服务器所执行的写命令来记录数据库状态的:  
![AOF持久化](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redis-AOF.png)

#### AOF持久化的实现

AOF持久化功能的实现可以分为命令追加、文件写入、文件同步三个步骤.  

- 命令追加
当AOF持久化功能处于打开状态时,服务器在执行完一个写命令之后,会以协议格式将被执行的写命令追加到服务器状态的aof_buf缓冲区的末尾:
```
struct redisServer {
//  ...

 sds aof_buf; /* AOF buffer, written before entering the event loop */
    
//  ...       
}
```
举个例子,如果客户端向服务器发送如下命令:  
```
redis> SET KEY VALUE
OK
```
那么服务器在执行这个SET命令之后,会将以下协议内容追加到aof_buf缓冲区的末尾:    
```
*3\r\n$3\r\nSET\r\n$3\r\bKEY\r\n$5\r\nVALUE\r\n        
```  
又例如,客户端想服务器发送以下命令:
```
redis> RPUSH NUMBERS ONE TWO THREE
(integer) 3
```
那么服务器在执行这个RPUSH命令之后,会将以下协议内容追加到aof_buf的末尾:  
```
*5\r\n$5\r\nRPUSH\r\n$7\rNUMBERS\r\n$3\r\nONE\r\n$3\r\nTWO\r\n$5\r\nTHREE\r\n  
```
以上就是AOF持久化的命令追加步骤的实现原理.

- AOF文件的写入与同步
redis的服务器进程就是一个事件循环,这个循环中文件事件负责接收客户端的命令请求,以及向客户端发送命令回复,而时间事件则负责执行像serverCron函数这样需要定时运行的函数.  
因为服务器在处理文件事件时可能会执行写命令,使得一些内容被追加到aof_buf缓冲区里面,所以在服务器每次结束一个事件循环之前,它都会调用flushAppendOnlyFile函数,考虑是否需要将aof_buf缓冲区中的内容写入和保存到AOF文件中里面.    
flushAppendOnlyFile函数的行为由服务器配置的appendfsync选项的值(默认everysec)来决定,各个不同值产生的行为如下:
![redis-AOF-sync](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redis-AOF-sync.png)  



