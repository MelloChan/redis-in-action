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

- 过期键删除策略  

1.定时删除:在设置键的过期时间的同时,创建了一个定时器,让定时器在键的过期时间来临时,立即执行对的键的删除操作:  
优点:内存友好,保证过期键不会占用内存空间;    
缺点:CPU不友好,如果过期键太多而导致CPU花费过多时间在删除和当前任务无关的过期键上,将影响服务器的性能.另外创建定时器需要用到redis服务器中的时间事件,而当前时间事件
的实现方式--无序链表,查找一个事件的时间复杂度为O(N),这并不能高效的处理大量时间事件.  
 
2.惰性删除:只有在键空间获取键的时候检查取得的键是否过期,过期则删除,否则返回:  
优点:CPU友好,显然在访问时才检查是否删除键可以让CPU不会花费时间在无关键上;  
缺点:内存不友好,如果有大量过期键存在,而恰好这些过期键也不常访问的话,就会导致过期键占用的内存不被释放(内存泄露);    

3.定期删除:每隔一段时间,程序就会对数据库进行一次检查,删除里面的过期键.至于要删除多少过期键以及要检查多少个数据库,则由算法决定.是定时策略与惰性删除的一种整合与折中方案:  
优点:通过定期删除策略每隔一段时间执行一次删除过期键操作,并通过限制删除操作执行的时长和频率来减少删除操作对CPU时间的影响.除此之外,通过定期删除过期键,能有效减少因为过期键带来的内存浪费;    
缺点:定期删除算法的实现,算法制定者必须根据服务器情况合理设置删除操作的时长和执行频率.    

- redis的过期键删除策略  

redis实际使用的是惰性删除策略与定期删除策略:通过配合使用两种策略,服务器可以很好的在合理使用CPU时间和避免浪费内存空间之间取得平衡.  
这一部分可查阅[db.c/expireIfNeeded方法](https://github.com/antirez/redis/blob/unstable/src/db.c):  
```
// 惰性删除策略
int expireIfNeeded(redisDb *db, robj *key) {
    mstime_t when = getExpire(db,key);
    mstime_t now;

    if (when < 0) return 0; /* No expire for this key */

    /* Don't expire anything while loading. It will be done later. */
    if (server.loading) return 0;

    /* If we are in the context of a Lua script, we pretend that time is
     * blocked to when the Lua script started. This way a key can expire
     * only the first time it is accessed and not in the middle of the
     * script execution, making propagation to slaves / AOF consistent.
     * See issue #1525 on Github for more information. */
    now = server.lua_caller ? server.lua_time_start : mstime();

    /* If we are running in the context of a slave, return ASAP:
     * the slave key expiration is controlled by the master that will
     * send us synthesized DEL operations for expired keys.
     *
     * Still we try to return the right information to the caller,
     * that is, 0 if we think the key should be still valid, 1 if
     * we think the key is expired at this time. */
    if (server.masterhost != NULL) return now > when;

    /* Return when this key has not expired */
    if (now <= when) return 0;

    /* Delete the key */
    server.stat_expiredkeys++;
    propagateExpire(db,key,server.lazyfree_lazy_expire);
    notifyKeyspaceEvent(NOTIFY_EXPIRED,
        "expired",key,db->id);
    return server.lazyfree_lazy_expire ? dbAsyncDelete(db,key) :
                                         dbSyncDelete(db,key);
}

// 定期删除策略
void activeExpireCycle(int type) {
    /* This function has some global state in order to continue the work
     * incrementally across calls. */
    static unsigned int current_db = 0; /* Last DB tested. */
    static int timelimit_exit = 0;      /* Time limit hit in previous call? */
    static long long last_fast_cycle = 0; /* When last fast cycle ran. */

    int j, iteration = 0;
    int dbs_per_call = CRON_DBS_PER_CALL;
    long long start = ustime(), timelimit, elapsed;

    /* When clients are paused the dataset should be static not just from the
     * POV of clients not being able to write, but also from the POV of
     * expires and evictions of keys not being performed. */
    if (clientsArePaused()) return;

    if (type == ACTIVE_EXPIRE_CYCLE_FAST) {
        /* Don't start a fast cycle if the previous cycle did not exit
         * for time limt. Also don't repeat a fast cycle for the same period
         * as the fast cycle total duration itself. */
        if (!timelimit_exit) return;
        if (start < last_fast_cycle + ACTIVE_EXPIRE_CYCLE_FAST_DURATION*2) return;
        last_fast_cycle = start;
    }

    /* We usually should test CRON_DBS_PER_CALL per iteration, with
     * two exceptions:
     *
     * 1) Don't test more DBs than we have.
     * 2) If last time we hit the time limit, we want to scan all DBs
     * in this iteration, as there is work to do in some DB and we don't want
     * expired keys to use memory for too much time. */
    if (dbs_per_call > server.dbnum || timelimit_exit)
        dbs_per_call = server.dbnum;

    /* We can use at max ACTIVE_EXPIRE_CYCLE_SLOW_TIME_PERC percentage of CPU time
     * per iteration. Since this function gets called with a frequency of
     * server.hz times per second, the following is the max amount of
     * microseconds we can spend in this function. */
    timelimit = 1000000*ACTIVE_EXPIRE_CYCLE_SLOW_TIME_PERC/server.hz/100;
    timelimit_exit = 0;
    if (timelimit <= 0) timelimit = 1;

    if (type == ACTIVE_EXPIRE_CYCLE_FAST)
        timelimit = ACTIVE_EXPIRE_CYCLE_FAST_DURATION; /* in microseconds. */

    /* Accumulate some global stats as we expire keys, to have some idea
     * about the number of keys that are already logically expired, but still
     * existing inside the database. */
    long total_sampled = 0;
    long total_expired = 0;

    for (j = 0; j < dbs_per_call && timelimit_exit == 0; j++) {
        int expired;
        redisDb *db = server.db+(current_db % server.dbnum);

        /* Increment the DB now so we are sure if we run out of time
         * in the current DB we'll restart from the next. This allows to
         * distribute the time evenly across DBs. */
        current_db++;

        /* Continue to expire if at the end of the cycle more than 25%
         * of the keys were expired. */
        do {
            unsigned long num, slots;
            long long now, ttl_sum;
            int ttl_samples;
            iteration++;

            /* If there is nothing to expire try next DB ASAP. */
            if ((num = dictSize(db->expires)) == 0) {
                db->avg_ttl = 0;
                break;
            }
            slots = dictSlots(db->expires);
            now = mstime();

            /* When there are less than 1% filled slots getting random
             * keys is expensive, so stop here waiting for better times...
             * The dictionary will be resized asap. */
            if (num && slots > DICT_HT_INITIAL_SIZE &&
                (num*100/slots < 1)) break;

            /* The main collection cycle. Sample random keys among keys
             * with an expire set, checking for expired ones. */
            expired = 0;
            ttl_sum = 0;
            ttl_samples = 0;

            if (num > ACTIVE_EXPIRE_CYCLE_LOOKUPS_PER_LOOP)
                num = ACTIVE_EXPIRE_CYCLE_LOOKUPS_PER_LOOP;

            while (num--) {
                dictEntry *de;
                long long ttl;

                if ((de = dictGetRandomKey(db->expires)) == NULL) break;
                ttl = dictGetSignedIntegerVal(de)-now;
                if (activeExpireCycleTryExpire(db,de,now)) expired++;
                if (ttl > 0) {
                    /* We want the average TTL of keys yet not expired. */
                    ttl_sum += ttl;
                    ttl_samples++;
                }
                total_sampled++;
            }
            total_expired += expired;

            /* Update the average TTL stats for this database. */
            if (ttl_samples) {
                long long avg_ttl = ttl_sum/ttl_samples;

                /* Do a simple running average with a few samples.
                 * We just use the current estimate with a weight of 2%
                 * and the previous estimate with a weight of 98%. */
                if (db->avg_ttl == 0) db->avg_ttl = avg_ttl;
                db->avg_ttl = (db->avg_ttl/50)*49 + (avg_ttl/50);
            }

            /* We can't block forever here even if there are many keys to
             * expire. So after a given amount of milliseconds return to the
             * caller waiting for the other active expire cycle. */
            if ((iteration & 0xf) == 0) { /* check once every 16 iterations. */
                elapsed = ustime()-start;
                if (elapsed > timelimit) {
                    timelimit_exit = 1;
                    server.stat_expired_time_cap_reached_count++;
                    break;
                }
            }
            /* We don't repeat the cycle if there are less than 25% of keys
             * found expired in the current DB. */
        } while (expired > ACTIVE_EXPIRE_CYCLE_LOOKUPS_PER_LOOP/4);
    }

    elapsed = ustime()-start;
    latencyAddSampleIfNeeded("expire-cycle",elapsed/1000);

    /* Update our estimate of keys existing but yet to be expired.
     * Running average with this sample accounting for 5%. */
    double current_perc;
    if (total_sampled) {
        current_perc = (double)total_expired/total_sampled;
    } else
        current_perc = 0;
    server.stat_expired_stale_perc = (current_perc*0.05)+
                                     (server.stat_expired_stale_perc*0.95);
}
```  

- AOF、RDB和复制功能对过期键的处理  

1.RDB:  
生成RDB文件:在执行save或bgsave命令创建一个新的RDB文件时,程序会对数据库中的键进行检查,过期键不会被保存到新创建的RDB文件中.因此过期键不会影响新创建的RDB文件;    
载入RDB文件:如果当前redis以主服务器运行则不会载入过期键,如果是从服务器则会载入,但过期键不会影响从服务器RDB文件的载入,因为根据主从服务器的特性,进行数据同步时,从服务器的数据将被清空;    

2.AOF:    
AOF文件写入:不受过期键影响,会在被删除的过期键中追加del命令到文件中,以此显示记录该键已被删除;      
AOF重写:不受过期键影响;    

3.复制功能:  
当服务器在复制模式中运行时,从服务器的过期键删除动作由主服务器控制:  
1.主服务器在删除一个过期键之后,会显示地向从服务器发送一个del命令,告知从服务器删除这个过期键;  
2.从服务器在执行客户端发送的读命令时,即使碰到过期键也不会删除,而是像处理未过期的键一样来处理过期键;  
3.从服务器只有在接收到主服务器的del命令后才会删除过期键;    
以上三特性保证了主从服务器间的数据一致性.    

- 数据库通知  

数据库通知功能可以让客户端通过订阅给定的频道或模式来获知数据库中键的变化,以及数据库中命令的执行情况.具体实现可参考[notify.c/notifyKeyspaceEvent](https://github.com/antirez/redis/blob/unstable/src/notify.c)
```
void notifyKeyspaceEvent(int type, char *event, robj *key, int dbid) {
    sds chan;
    robj *chanobj, *eventobj;
    int len = -1;
    char buf[24];

    /* If any modules are interested in events, notify the module system now. 
     * This bypasses the notifications configuration, but the module engine
     * will only call event subscribers if the event type matches the types
     * they are interested in. */
     moduleNotifyKeyspaceEvent(type, event, key, dbid);
    
    /* If notifications for this class of events are off, return ASAP. */
    if (!(server.notify_keyspace_events & type)) return;

    eventobj = createStringObject(event,strlen(event));

    /* __keyspace@<db>__:<key> <event> notifications. */
    if (server.notify_keyspace_events & NOTIFY_KEYSPACE) {
        chan = sdsnewlen("__keyspace@",11);
        len = ll2string(buf,sizeof(buf),dbid);
        chan = sdscatlen(chan, buf, len);
        chan = sdscatlen(chan, "__:", 3);
        chan = sdscatsds(chan, key->ptr);
        chanobj = createObject(OBJ_STRING, chan);
        pubsubPublishMessage(chanobj, eventobj);
        decrRefCount(chanobj);
    }

    /* __keyevent@<db>__:<event> <key> notifications. */
    if (server.notify_keyspace_events & NOTIFY_KEYEVENT) {
        chan = sdsnewlen("__keyevent@",11);
        if (len == -1) len = ll2string(buf,sizeof(buf),dbid);
        chan = sdscatlen(chan, buf, len);
        chan = sdscatlen(chan, "__:", 3);
        chan = sdscatsds(chan, eventobj->ptr);
        chanobj = createObject(OBJ_STRING, chan);
        pubsubPublishMessage(chanobj, key);
        decrRefCount(chanobj);
    }
    decrRefCount(eventobj);
}
```