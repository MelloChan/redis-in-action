### RDB持久化  

因为Redis是内存数据库,它将自己的数据库状态储存在内存里面,所以如果不想办法将内存中的数据库状态保存到磁盘里面,那么一旦服务器进程退出,服务器中的数据库状态也会消失不见.  
为了解决这个问题,Redis提供了RDB持久化功能,这个功能可以将Redis在内存中的数据库状态保存到磁盘里,避免数据意外丢失.    
RDB持久化既可以手动也可以根据服务器配置选项定期执行,该功能可以将某个时间点上数据库状态保存到一个RDB文件中.生成的二进制RDB文件还可以还原生成RDB文件的数据库状态,如下图所示:
![RDB](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/RDB保存与还原.png)  

因为RDB文件是保存在硬盘里面的,所以即使Redis服务器进程退出,甚至运行Redis服务器的计算机停机,只要RDB文件存在,Redis服务器就可以用它来还原数据库状态.  

#### RDB文件的创建与载入  

生成RDB文件的命令有:    
1.SAVE:该命令会阻塞Redis服务器进程,直到RDB文件创建完毕为止,在服务器进程阻塞期间,服务器不能处理任何命令请求.    
2.BGSAVE:该命令会派生出一个子进程,然后由子进程负责创建RDB文件,服务器进程继续处理命令请求.    
创建RDB文件的实际工作则有 [rdb.c/rdbSave](https://github.com/antirez/redis/blob/unstable/src/rdb.c) 函数完成.
和使用SAVE命令或BGSAVE命令创建RDB文件不同,RDB文件的载入工作是在服务器启动时自动执行的,所以Redis并没有专门用于载入RDB文件的命令,只要Redis在启动时检测到RDB文件存在,它就会自动载入RDB文件.这一点在启动redis-server打印的日志记录就可知:
```
# Server started, Redis version 3.2.100
* DB loaded from disk: 0.012 seconds
* The server is now ready to accept connections on port 6379
```
另外,因为AOF文件的更新频率通常比RDB文件的更新频率高,所以如果服务器开启了AOF持久化,则服务器优先使用AOF文件来还原数据库状态,只有AOF持久化功能关闭时,服务器才会使用RDB.  
载入RDB文件的实际工作由 rdb.c/rdbLoad 函数完成.
```
int rdbLoad(char *filename, rdbSaveInfo *rsi) {
    FILE *fp;
    rio rdb;
    int retval;

    if ((fp = fopen(filename,"r")) == NULL) return C_ERR;
    startLoading(fp);
    rioInitWithFile(&rdb,fp);
    retval = rdbLoadRio(&rdb,rsi);
    fclose(fp);
    stopLoading();
    return retval;
}
```
服务器判断用哪个文件来还原数据库状态的流程以及 rdbLoad 和 rdbSave 函数的关系如下:  
![RDB](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/Redis-RDB-save&load.png)

另外,执行BGSAVE时,客户端发送的SAVE(BGSAVE)命令将会被服务器拒绝执行,这是为了防止同时调用 rdbSave函数而导致产生竞争条件.同时在载入RDB文件期间,服务器会处于阻塞状态,直到工作完成.

#### 自动间隔性保存  

因为BGSAVE命令可以在不阻塞服务器进程的情况下执行,所以Redis允许用户通过设置服务配置的save选项,让服务器每隔一段时间自动执行一次BGSAVE命令.  
用户可以通过save选项设置多个保存条件,但只要任意一个条件被满足,服务器就会执行BGSAVE命令.服务器默认配置如下:    
```
save 900 1    // 服务器在900秒之内,对数据库进行了至少1次修改
save 300 10   // 服务器在300秒之内,对数据库进行了至少10次修改
save 60 1000  // 服务器在60秒之内,对数据库进行了至少1000次修改
```  
配置文件设置的save选项保存在[server.h](https://github.com/antirez/redis/blob/unstable/src/server.h)文件中,根据save选项设置saveparams属性:
```
struct redisServer {
//  ...

struct saveparam *saveparams; /* Save points array for RDB */

//  ...
}
struct saveparam {
    time_t seconds; // 秒数
    int changes;    // 修改数
};
```  

除了saveparams数组之外,服务器状态还维持着一个dirty计数器,以及lastsave属性:
```
struct redisServer {
//  ...

    long long dirty;                /* 计数器记录距离上一次成功执行SAVE命令或BGSAVE命令之后,服务器对数据库状态(服务器中的所有数据库)进行了多少次修改(包括写入、删除以及更新等操作) */
   
   
    struct saveparam *saveparams;   /* Save points array for RDB */
  
    time_t lastsave; /*  Unix时间戳,记录了服务器上一次成功执行SAVE或BGSAVE命令的时间  */
    
//  ...       
}
```
当服务器成功执行一个数据库修改命令之后,程序就会对dirty计数器进行更新:命令修改了多少次数据库,dirty计数器的值就增加多少.  
例如,如果我们为一个字符串设置值:
```
redis> SET msg "hello"
OK
```
那么dirty计数器的值将+1,而如果我们又向一个集合增加三个新元素:  
```
redis> SADD database Redis MongoDB MariaDB
(integer) 3
```
那么dirty计数器的值将+3.  

#### 检查条件是否满足

Redis的服务器周期性操作函数 serverCron 默认每个100毫秒就会执行一次,该函数用于对正在运行的服务器进行维护,它的其中一项工作就是检查save选项所设置的条件(遍历检查saveparams数组)是否满足,如果满足,就会执行BGSAVE命令.  

#### RDB文件结构

![RDB文件结构](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redis-RDB文件结构.png)








