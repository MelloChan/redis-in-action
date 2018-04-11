### 数据库  

- 服务器中的数据库  

redis服务器将所有的数据库都保存在服务器状态 [server.h](https://github.com/antirez/redis/blob/unstable/src/server.h)/redisServer结构的db数组中,db数组的每个项都是一个server.h/redisDb结构:  

![redisServer](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redisServer-db.png)  

在初始化服务器时,程序会根据服务器状态的dbnum属性来决定应该创建多少个数据库:  
  
![redisServer](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redisServer-dbnum.png)

- 切换数据库

每个redis客户端都有自己的目标数据库,每当客户端执行数据库写命令或者数据库读命令时,目标数据库就会成为这些命令的操作对象.  
默认情况下,redis客户端的目标数据库为0号数据库,但客户端可以通过执行select命令来切换目标数据库:   

![select](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/select-dbnum.png)

在服务器内部,客户端状态redisClient结构的db属性记录了客户端当前的目标数据库,这个属性时一个指向redisDb结构的指针:  

![client](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redisServer-client.png)  

切换过程就是client中redisDb指针的改变:
![select](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/select-db.png)  