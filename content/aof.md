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

#### AOF文件的载入与数据还原  

因为AOF文件里面包含了重建数据库状态所需的所有写命令,所以服务器只要读入并重新执行一遍AOF文件里面保存的写命令,就可以还原服务器关闭之前的数据库状态.  
Redis读取AOF文件并还原数据库状态的步骤如下:    
![载入与还原](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redis-AOF-load.png)

#### AOF重写  

因为AOF持久化是通过保存被执行的写命令来记录数据库状态的,所以随着服务器运行时间的流逝,AOF文件中的内容会越来越多,文件的体积也会越来越大,如果不加以控制的话,体积过大的AOF文件很可能对Redis服务器、甚至宿主计算机造成影响,并且AOF文件的体积越大,使用AOF文件来进行数据还原所需的时间就越多.  
因此为了解决AOF文件体积膨胀的问题,Redis提供了AOF文件重写的功能.通过该功能,Redis服务器可以创建一个新的AOF文件来替代现有的AOF文件,新旧两个AOF文件所保存的数据库状态相同,但AOF文件不会包含任何浪费的冗余命令,所以新文件的体积通常会比旧文件小很多.  

重写技术通过从数据库中读取键值对,然后对原先的命令进行缩减保存到新的AOF文件中,这个过程不需要去读取旧AOF文件.例如:
```
redis> RPUSH list "A" "B"
(integer) 2
redis> RPUSH list "C"
(integer) 3
redis> RPUSH list "D" "E"
(integer) 5
redis> LPOP list
"A"
redis> LPOP list
"B"
redis> RPUSH list "F" "G"
(integer) 5
```
如果服务器为了保存当前list的状态,必须在AOF文件中写入六条命令,但如果想用尽量少的命令来记录的话,可以直接从数据库中读取键list的值,然后用一条命令来替代六条命令:
```
redis> RPUSH list "C" "D" "E" "F" "G"
```
这样就大大减少了AOF文件保存的写命令条数了.这个重写过程一般是在后台进行的,这是为了防止调用这个函数的线程被长时间阻塞而导致服务器无法接收客户端发来的命令请求.  
但是,另起子进程进行后台重写会导致一个问题,AOF重写期间,服务器进程因新命令而改变了数据库状态,从而使得服务器当前的数据库状态和重写后的AOF文件所保存的数据库状态不一致.  
为了解决不一致问题,Redis服务器设置了一个AOF重写缓冲区,这个缓冲区在服务器创建子进程之后开始使用,当Redis服务器执行完一个写命令之后,它会同时将这个写命令发送给AOF缓冲区与AOF重写缓冲区.  
这样就可以保证:  
- AOF缓冲区的内容会定期被写入和同步到AOF文件中,对现有AOF文件的处理工作会如常进行    
- 从创建子进程开始,服务器执行的所有写命令都会被记录到AOF重写缓冲区里面     

当子进程完成AOF重写工作之后,它会向发进程发送一个信号,父进程在接到该信号之后,会调用一个信号处理函数,并执行以下工作:  
- 将AOF重写缓冲区中的所有内容写入到新AOF文件中,这时新的AOF文件所保存的数据库状态将和服务器当前的数据库状态一致    
- 对新的AOF文件进行改名,原子地覆盖现有的AOF文件,完成新旧两个AOF文件的替换    

这个信号处理函数执行完毕之后,父进程就可以继续像往常一样接受命令参数了.   
在整个AOF后台重写过程中,只有信号处理函数执行时会对服务器进程造成阻塞,在其他时候,AOF后台重写都不会阻塞服务器进程,这将AOF重写对服务器性能造成的影响降到了最低.



