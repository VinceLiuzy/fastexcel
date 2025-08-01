# 大数据量文件

## 读取

### 概述
当需要读取 10M 大小以上的文件时,Excel 03 没有办法处理，相对内存占用大很多。Excel 07 版本有个共享字符串[共享字符串](https://docs.microsoft.com/zh-cn/office/open-xml/working-with-the-shared-string-table)的概念，这个会非常占用内存，如果全部读取到内存的话，大概是 Excel 文件的大小的 3-10 倍，所以 FastExcel 用先存储文件的，然后再反序列化去读取的策略来节约内存。当然需要通过文件反序列化以后，效率会降低，大概降低 30-50%（不一定，也看命中率，可能会超过100%）。

如果对读取效率感觉还能接受，就用默认的，永久占用（单个 Excel 读取整个过程）一般不会超过50M(大概率就30M)，剩下临时的 GC 会很快回收。

### 默认策略
默认大文件处理会自动判断，共享字符串 5M 以下会使用内存存储，大概占用 15-50M 的内存,超过 5M 则使用文件存储，然后文件存储也要设置多内存用来存放临时的共享字符串，默认 20M。除了共享字符串占用内存外，其他占用较少，所以可以预估 10M，所以默认大概 30M 就能读取一个超级大的文件。

### 配置内存
如果想自定义设置，首先要确定大概愿意花多少内存来读取一个超级大的 Excel,比如希望读取 Excel 最多占用 100M 内存（是读取过程中永久占用，新生代马上回收的不算），那就设置使用文件来存储共享字符串的大小判断为 20M (小于20M存内存，大于存临时文件)，然后设置文件存储时临时共享字符串占用内存大小 90M 差不多。

如果最大文件条数也就十几二十万，然后 Excel 也就是十几二十M，而且不会有很高的并发，内存也较大
```java
// 强制使用内存存储，这样大概一个 20M 的 Excel 使用 150M（很多临时对象，所以100M会一直GC）的内存
// 这样效率会比上面的复杂的策略高很多
// 这里再说明下 就是加了个readCache(new MapCache()) 参数而已，其他的参照其他示例写
FastExcel.read().readCache(new MapCache());
```

对并发要求较高，而且都是经常有超级大文件
```java
// 第一个参数的意思是多少M共享字符串以后，采用文件存储（单位MB，默认5M）
// 第二个参数 文件存储时，内存存放多少M缓存数据，默认20M
// 比如 你希望用 100M 内存(这里说的是解析过程中的永久占用,临时对象不算)来解析excel，前面算过了，大概是 20M+90M，所以设置参数为:20 和 90
// 这里再说明下 就是加了个 readCacheSelector(new SimpleReadCacheSelector(5, 20)) 参数而已，其他的参照其他示例写
FastExcel.read().readCacheSelector(new SimpleReadCacheSelector(5, 20));
```

### 关于 maxCacheActivateSize
FastExcel 在使用文件存储的时候，会把共享字符串拆分成 **1000** 条一批，然后放到文件存储。然后 Excel 来读取共享字符串大概率是按照顺序的，所以默认 20M 的 1000 条的数据放在内存，命中后直接返回，没命中去读文件。所以不能设置太小，太小了，很难命中，一直去读取文件，太大了的话会占用过多的内存。

判断 maxCacheActivateSize是否需要调整，开启`debug`日志会输出`Already put :4000000` 最后一次输出，大概可以得出值为 400W,然后看`Cache misses count:4001`得到值为 4K，`400W/4K=1000` 这代表已经 `maxCacheActivateSize` 已经非常合理了。如果小于 500 问题就非常大了，500 到 1000 应该都还行。
