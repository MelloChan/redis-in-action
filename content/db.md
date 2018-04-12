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

- 数据库键空间

redis是一个键值对数据库服务器,服务器中的每个数据库都由一个redisDb结构表示,其中,redisDb结构表示,其中,redisDb结构的dict字典保存了
数据库中的所有键值对,我们将这个字典称为键空间:  

![dict](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redisServer-redisDb.png)  
键空间和用户所见的数据库是直接对应的:  
1.键空间的键也是数据库的键,每个键都是一个字符串对象.  
2.键空间的值也是数据库的值,每个值可用是字符串对象、列表对象、哈希表对象、集合对象和有序集合对象中的任意一种redis对象.  
![keySpace](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redis-keySpace.png)  
![keySpace](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/redis-keySpace-structure.png)  

- 设置键的生存时间或过期时间  

通过expire或pexpire命令,客户端可以以秒或毫秒精度为数据库中的某个键设置生存时间(指定多少秒或毫秒后),在经过指定的秒数或毫秒之后,服务器就会自动删除生存时间为0的键:  
![expire](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/expire.png)  

另外,也可以通过expireat或pexpireat命令,以秒或毫秒精度给数据库中的某个键设置过期时间(指定一个时间,设置时间戳).通过time查阅当然时间,ttl命令来查阅键的剩余时间.
![expireat](https://raw.githubusercontent.com/MelloChan/redis-in-action/master/images/expireat-time-ttl.png)